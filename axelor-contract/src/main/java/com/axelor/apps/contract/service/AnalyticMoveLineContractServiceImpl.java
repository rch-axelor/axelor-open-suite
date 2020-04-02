package com.axelor.apps.contract.service;

import java.util.List;

import com.axelor.apps.account.db.AnalyticMoveLine;
import com.axelor.apps.account.service.AccountManagementServiceAccountImpl;
import com.axelor.apps.account.service.app.AppAccountService;
import com.axelor.apps.contract.db.ContractLine;
import com.axelor.apps.supplychain.service.AnalyticMoveLineSupplychainServiceImpl;
import com.axelor.rpc.Context;
import com.google.inject.Inject;

public class AnalyticMoveLineContractServiceImpl extends AnalyticMoveLineSupplychainServiceImpl {

  @Inject
  public AnalyticMoveLineContractServiceImpl(
      AppAccountService appAccountService,
      AccountManagementServiceAccountImpl accountManagementServiceAccountImpl) {
    super(appAccountService, accountManagementServiceAccountImpl);
  }

  @Override
  public List<AnalyticMoveLine> getAnalyticMoveLineList(Context parentContext) {

    List<AnalyticMoveLine> list = super.getAnalyticMoveLineList(parentContext);
    Class<?> klass = parentContext.getContextClass();
    if (klass.isAssignableFrom(ContractLine.class)) {
      list = parentContext.asType(ContractLine.class).getAnalyticMoveLineList();
    }
    return list;
  }
}
