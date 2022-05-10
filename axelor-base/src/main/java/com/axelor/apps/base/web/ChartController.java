/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2022 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.apps.base.web;

import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.base.service.user.UserService;
import com.axelor.auth.db.User;
import com.axelor.common.ObjectUtils;
import com.axelor.db.JPA;
import com.axelor.exception.AxelorException;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaView;
import com.axelor.meta.db.repo.MetaViewRepository;
import com.axelor.meta.loader.XMLViews;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.meta.schema.actions.ActionView.ActionViewBuilder;
import com.axelor.meta.schema.views.ChartView;
import com.axelor.meta.schema.views.ChartView.ChartCategory;
import com.axelor.meta.schema.views.ChartView.ChartConfig;
import com.axelor.meta.schema.views.ChartView.ChartSeries;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.common.base.CaseFormat;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.persistence.Query;
import org.apache.commons.lang3.StringUtils;

public class ChartController {

  @Inject AppBaseService appBaseService;
  @Inject MetaViewRepository metaViewRepo;

  private static final String QUERY_PATTERN = "(?i)(select)(.+?)(from)";
  private static final String QUERY_GROUP_BY_PATTERN = "(?i)(group by)(?!.*(group by))(.+)";
  private static final String QUERY_ORDER_BY_PATTERN = "(?i)(order by)(?!.*(order by))(.+)";
  private static final String QUERY_USER_PATTERN = "(?i)(\\.id)(\\s=\\s)(:__user__)";
  private static final String QUERY_DATE_PATTERN = ":__date__";
  private static final String QUERY_DATE_TIME_PATTERN = ":__datetime__";

  @SuppressWarnings("unchecked")
  public void chartOnClick(ActionRequest request, ActionResponse response) throws AxelorException {

    Map<String, Object> data = request.getData();
    Map<String, Object> context = (Map<String, Object>) data.get("context");
    if (!context.containsKey("_chart")) {
      return;
    }

    String chartName = context.get("_chart").toString();
    ChartView chartView = (ChartView) XMLViews.findView(chartName, "chart");
    if (chartView == null) {
      return;
    }

    String title = chartView.getTitle();
    String modelFullName = null;
    List<ChartConfig> chartConfigs = chartView.getConfig();
    if (ObjectUtils.notEmpty(chartConfigs)) {
      Optional<ChartConfig> modelConfigOpt =
          chartConfigs.stream().filter(config -> config.getName().equals("_model")).findFirst();
      if (modelConfigOpt.isPresent()) {
        modelFullName = modelConfigOpt.get().getValue();
      }
    }
    String modelName =
        modelFullName == null ? null : StringUtils.substringAfterLast(modelFullName, ".");
    String viewName =
        modelName == null ? null : CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, modelName);

    String filter = "self.id IN :ids";
    List<Long> ids = getIdList(context, chartView);

    ActionViewBuilder actionViewBuilder = ActionView.define(I18n.get(title));
    actionViewBuilder.model(modelFullName);
    actionViewBuilder.add("grid", getViewName(viewName, "grid", modelFullName));
    actionViewBuilder.add("form", getViewName(viewName, "form", modelFullName));
    actionViewBuilder.domain(filter);
    actionViewBuilder.context("ids", ids);
    response.setView(actionViewBuilder.map());
  }

  protected String getViewName(String viewName, String type, String model) {

    String view = String.format("%s-%s", viewName, type);
    MetaView metaView = metaViewRepo.findByName(view);
    if (metaView == null) {
      metaView = metaViewRepo.all()
          .filter("self.type = :type AND self.model = :model")
          .bind("type", type)
          .bind("model", model)
          .fetchOne();
    }

    return metaView != null ? metaView.getName() : view;
  }

  @SuppressWarnings("unchecked")
  protected List<Long> getIdList(Map<String, Object> context, ChartView chartView) {

    User user = Beans.get(UserService.class).getUser();

    ChartCategory chartCategory = chartView.getCategory();
    String filterValue = null;
    if (context.containsKey(chartCategory.getKey())) {
      filterValue = context.get(chartCategory.getKey()).toString();
    }

    ChartSeries chartSeries = chartView.getSeries().get(0);
    String groupFilterValue = null;
    if (StringUtils.isNotBlank(chartSeries.getGroupBy())
        && context.containsKey(chartSeries.getGroupBy())) {
      groupFilterValue = context.get(chartSeries.getGroupBy()).toString();
    }

    String filterKey = null;
    String groupFilterKey = null;
    List<ChartConfig> chartConfigs = chartView.getConfig();
    if (ObjectUtils.notEmpty(chartConfigs)) {
      Optional<ChartConfig> filterConfigOpt =
          chartConfigs.stream().filter(config -> config.getName().equals("_filter")).findFirst();
      if (filterConfigOpt.isPresent()) {
        filterKey = filterConfigOpt.get().getValue();
      }

      Optional<ChartConfig> groupFilterConfigOpt = chartConfigs.stream()
          .filter(config -> config.getName().equals("_groupFilter"))
          .findFirst();
      if (groupFilterConfigOpt.isPresent()) {
        groupFilterKey = groupFilterConfigOpt.get().getValue();
      }
    }

    String whereFilter = String.format("(%s = :chartFilterValue)", filterKey);
    if (StringUtils.isNotBlank(groupFilterKey)) {
      whereFilter += String.format(" AND (%s = :chartGroupFilterValue) ", groupFilterKey);
    }

    String queryStr = StringUtils
        .normalizeSpace(chartView.getDataSet().getText().trim().replaceAll("\\n", " "))
        .replaceFirst(QUERY_PATTERN, "$1 self.id $3")
        .replaceAll(QUERY_ORDER_BY_PATTERN, " ")
        .replaceAll(QUERY_GROUP_BY_PATTERN, String.format(" AND %s $1 self.id", whereFilter))
        .replaceAll(QUERY_USER_PATTERN, String.format("$1$2%s", user.getId()))
        .replaceAll(QUERY_DATE_PATTERN, String.format("'%s'", appBaseService.getTodayDate(null)))
        .replaceAll(
            QUERY_DATE_TIME_PATTERN,
            String.format("'%s'", appBaseService.getTodayDateTime(null)))
        .trim();
    Query query = null;
    if (chartView.getDataSet().getType().equals("sql")) {
      query = JPA.em().createNativeQuery(queryStr).setParameter("chartFilterValue", filterValue);
    } else if (chartView.getDataSet().getType().equals("jpql")) {
      query = JPA.em().createQuery(queryStr).setParameter("chartFilterValue", filterValue);
    } else {
      // rpc : TODO
    }

    if (StringUtils.isNotBlank(groupFilterValue)) {
      query = query.setParameter("chartGroupFilterValue", groupFilterValue);
    }

    if (query != null) {
      return query.getResultList();
    }

    return new ArrayList<>();
  }
}
