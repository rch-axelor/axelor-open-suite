package com.axelor.apps.hr.service.expense;

import java.util.List;

import com.axelor.apps.account.db.AnalyticMoveLine;
import com.axelor.apps.account.service.AccountManagementServiceAccountImpl;
import com.axelor.apps.account.service.AnalyticMoveLineServiceImpl;
import com.axelor.apps.account.service.app.AppAccountService;
import com.axelor.apps.hr.db.ExpenseLine;
import com.axelor.rpc.Context;

public class AnalyticMoveLineHRServiceImpl{ // extends AnalyticMoveLineServiceImpl {

  /*public AnalyticMoveLineHRServiceImpl(
      AppAccountService appAccountService,
      AccountManagementServiceAccountImpl accountManagementServiceAccountImpl) {
    super(appAccountService, accountManagementServiceAccountImpl);
  }

  @Override*/
  public List<AnalyticMoveLine> getAnalyticMoveLineList(Context parentContext) {

    List<AnalyticMoveLine> list = null; // super.getAnalyticMoveLineList(parentContext);
    Class<?> klass = parentContext.getContextClass();
    if (klass.isAssignableFrom(ExpenseLine.class)) {
      list = parentContext.asType(ExpenseLine.class).getAnalyticMoveLineList();
    }
    return list;
  }
}
