package com.axelor.apps.supplychain.service;

import java.util.List;

import com.axelor.apps.account.db.AnalyticMoveLine;
import com.axelor.apps.account.service.AccountManagementServiceAccountImpl;
import com.axelor.apps.account.service.AnalyticMoveLineServiceImpl;
import com.axelor.apps.account.service.app.AppAccountService;
import com.axelor.apps.purchase.db.PurchaseOrderLine;
import com.axelor.apps.sale.db.SaleOrderLine;
import com.axelor.rpc.Context;
import com.google.inject.Inject;

public class AnalyticMoveLineSupplychainServiceImpl extends AnalyticMoveLineServiceImpl {

  @Inject
  public AnalyticMoveLineSupplychainServiceImpl(
      AppAccountService appAccountService,
      AccountManagementServiceAccountImpl accountManagementServiceAccountImpl) {
    super(appAccountService, accountManagementServiceAccountImpl);
  }

  @Override
  public List<AnalyticMoveLine> getAnalyticMoveLineList(Context parentContext) {

    List<AnalyticMoveLine> list = super.getAnalyticMoveLineList(parentContext);
    Class<?> klass = parentContext.getContextClass();
    if (klass.isAssignableFrom(SaleOrderLine.class)) {
      list = parentContext.asType(SaleOrderLine.class).getAnalyticMoveLineList();
    } else if (klass.isAssignableFrom(PurchaseOrderLine.class)) {
      list = parentContext.asType(PurchaseOrderLine.class).getAnalyticMoveLineList();
    }
    return list;
  }
}
