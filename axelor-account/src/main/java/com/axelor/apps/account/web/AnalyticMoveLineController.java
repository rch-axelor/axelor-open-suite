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
package com.axelor.apps.account.web;

import java.util.ArrayList;
import java.util.List;

import com.axelor.apps.account.db.AnalyticMoveLine;
import com.axelor.apps.account.service.AnalyticMoveLineService;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;

public class AnalyticMoveLineController {

  public void validatePercentage(ActionRequest request, ActionResponse response) {

    AnalyticMoveLine analyticMoveLine = request.getContext().asType(AnalyticMoveLine.class);
    Context parentContext = request.getContext().getParent();
    List<AnalyticMoveLine> list =
        Beans.get(AnalyticMoveLineService.class).getAnalyticMoveLineList(parentContext);
    if (list == null) {
      list = new ArrayList<>();
    }
    list.add(analyticMoveLine);
    if (!Beans.get(AnalyticMoveLineService.class).validateAnalyticMoveLines(list)) {
      response.setError(
          "The distribution is wrong, some axes percentage values are higher than 100%");
    }
  }
}
