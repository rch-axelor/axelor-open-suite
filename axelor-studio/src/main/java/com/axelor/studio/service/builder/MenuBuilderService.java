/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2020 Axelor (<http://axelor.com>).
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
package com.axelor.studio.service.builder;

import com.axelor.apps.tool.QueryBuilder;
import com.axelor.common.Inflector;
import com.axelor.common.StringUtils;
import com.axelor.exception.service.TraceBackService;
import com.axelor.inject.Beans;
import com.axelor.meta.MetaStore;
import com.axelor.meta.db.MetaAction;
import com.axelor.meta.db.MetaJsonRecord;
import com.axelor.meta.db.MetaMenu;
import com.axelor.meta.db.MetaView;
import com.axelor.meta.loader.XMLViews;
import com.axelor.meta.schema.ObjectViews;
import com.axelor.meta.schema.actions.Action;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.studio.db.ActionBuilder;
import com.axelor.studio.db.ActionBuilderLine;
import com.axelor.studio.db.ActionBuilderView;
import com.axelor.studio.db.AppBuilder;
import com.axelor.studio.db.MenuBuilder;
import com.axelor.studio.db.repo.ActionBuilderRepository;
import com.axelor.studio.db.repo.MenuBuilderRepo;
import com.axelor.studio.service.StudioMetaService;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.xml.bind.JAXBException;

public class MenuBuilderService {

  @Inject private ActionBuilderService actionBuilderService;

  @Inject private StudioMetaService metaService;

  @Transactional
  public MetaMenu build(MenuBuilder builder) {

    MetaMenu menu = metaService.createMenu(builder);
    ActionBuilder actionBuilder = builder.getActionBuilder();
    if (actionBuilder != null) {
      if (actionBuilder.getName() == null) {
        actionBuilder.setName(menu.getName().replace("-", "."));
      }
      actionBuilder.setTitle(menu.getTitle());
      actionBuilder.setAppBuilder(builder.getAppBuilder());
      menu.setAction(actionBuilderService.build(actionBuilder));
    }

    MetaStore.clear();

    return menu;
  }

  @SuppressWarnings("unchecked")
  public Optional<ActionBuilder> createActionBuilder(MetaAction metaAction) {

    try {
      ObjectViews objectViews = XMLViews.fromXML(metaAction.getXml());
      List<Action> actions = objectViews.getActions();
      if (actions != null && !actions.isEmpty()) {
        ActionView action = (ActionView) actions.get(0);
        if (action.getModel() != null
            && action.getModel().contentEquals(MetaJsonRecord.class.getName())) {
          return Optional.empty();
        }
        ActionBuilder actionBuilder = new ActionBuilder(action.getName());
        actionBuilder.setTitle(action.getTitle());
        actionBuilder.setModel(action.getModel());
        actionBuilder.setTypeSelect(ActionBuilderRepository.TYPE_SELECT_VIEW);
        String domain = action.getDomain();
        actionBuilder.setDomainCondition(domain);
        for (ActionView.View view : action.getViews()) {
          ActionBuilderView builderView = new ActionBuilderView();
          builderView.setViewType(view.getType());
          builderView.setViewName(view.getName());
          actionBuilder.addActionBuilderView(builderView);
        }
        if (action.getParams() != null) {
          for (ActionView.Param param : action.getParams()) {
            ActionBuilderLine paramLine = new ActionBuilderLine();
            paramLine.setName(param.getName());
            paramLine.setValue(param.getValue());
            actionBuilder.addViewParam(paramLine);
          }
        }
        if (action.getContext() != null) {
          for (ActionView.Context ctx : (List<ActionView.Context>) action.getContext()) {
            ActionBuilderLine ctxLine = new ActionBuilderLine();
            ctxLine.setName(ctx.getName());
            if (ctx.getName().contentEquals("jsonModel")
                && domain != null
                && domain.contains("self.jsonModel = :jsonModel")) {
              actionBuilder.setIsJson(true);
              actionBuilder.setModel(ctx.getExpression());
            }
            ctxLine.setValue(ctx.getExpression());
            actionBuilder.addLine(ctxLine);
          }
        }

        return Optional.of(actionBuilder);
      }
    } catch (JAXBException e) {
      TraceBackService.trace(e);
    }
    return Optional.empty();
  }

  @Transactional
  public MenuBuilder updateMenuBuilder(
      MenuBuilder menuBuilder,
      String objectName,
      String menuName,
      AppBuilder appBuilder,
      String objectClass,
      Boolean isJson,
      String domain) {

    menuBuilder.setName(this.generateMenuBuilderName(menuName));
    menuBuilder.setAppBuilder(appBuilder);

    menuBuilder.setShowAction(true);
    ActionBuilder actionBuilder = menuBuilder.getActionBuilder();
    if (actionBuilder == null) {
      actionBuilder = new ActionBuilder();
    }
    actionBuilder.setTypeSelect(ActionBuilderRepository.TYPE_SELECT_VIEW);
    actionBuilder.setIsJson(isJson);
    actionBuilder.setModel(objectName);
    if (!Strings.isNullOrEmpty(domain)) {
      actionBuilder.setDomainCondition(domain);
    }
    menuBuilder.setActionBuilder(actionBuilder);
    setActionViews(actionBuilder, isJson, objectName, objectClass);

    return Beans.get(MenuBuilderRepo.class).save(menuBuilder);
  }

  private void setActionViews(
      ActionBuilder actionBuilder, Boolean isJson, String objectName, String objectClass) {

    List<ActionBuilderView> views = actionBuilder.getActionBuilderViews();
    if (views == null) {
      views = new ArrayList<>();
      actionBuilder.setActionBuilderViews(views);
    }

    String viewName = Inflector.getInstance().dasherize(objectName);
    String title = null;
    if (isJson) {
      title = objectName;
      viewName = "custom-model-" + objectName;
    }

    String gridName;
    MetaView gridView = this.getMetaView(objectClass, "grid", title);
    if (gridView != null) {
      gridName = gridView.getName();
    } else {
      gridName = viewName + "-grid";
    }
    this.setActionBuilderView("grid", gridName, views);

    String formName;
    MetaView formView = this.getMetaView(objectClass, "form", title);
    if (formView != null) {
      formName = formView.getName();
    } else {
      formName = viewName + "-form";
    }
    this.setActionBuilderView("form", formName, views);
  }

  private MetaView getMetaView(String model, String type, String title) {

    QueryBuilder<MetaView> metaViewQuery =
        QueryBuilder.of(MetaView.class)
            .add("self.model = :model")
            .add("self.type = :type")
            .bind("model", model)
            .bind("type", type);

    if (!StringUtils.isBlank(title)) {
      metaViewQuery.add("self.title = :title").bind("title", title);
    }

    return metaViewQuery.build().fetchOne();
  }

  private void setActionBuilderView(
      String viewType, String viewName, List<ActionBuilderView> actionBuilderViews) {

    ActionBuilderView actionBuilderView = new ActionBuilderView();
    actionBuilderView.setViewType(viewType);
    actionBuilderView.setViewName(viewName);
    actionBuilderViews.add(actionBuilderView);
  }

  private String generateMenuBuilderName(String name) {
    return "studio-menu-" + name.toLowerCase().replaceAll("[ ]+", "-");
  }
}
