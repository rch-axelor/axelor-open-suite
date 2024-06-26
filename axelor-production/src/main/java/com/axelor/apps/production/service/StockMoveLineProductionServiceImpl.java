package com.axelor.apps.production.service;

import com.axelor.apps.account.db.repo.InvoiceLineRepository;
import com.axelor.apps.base.service.PriceListService;
import com.axelor.apps.base.service.ProductCompanyService;
import com.axelor.apps.base.service.ShippingCoefService;
import com.axelor.apps.base.service.UnitConversionService;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.base.service.tax.AccountManagementService;
import com.axelor.apps.stock.db.StockMoveLine;
import com.axelor.apps.stock.db.TrackingNumber;
import com.axelor.apps.stock.db.repo.StockMoveLineRepository;
import com.axelor.apps.stock.db.repo.TrackingNumberRepository;
import com.axelor.apps.stock.service.StockLocationLineHistoryService;
import com.axelor.apps.stock.service.StockLocationLineService;
import com.axelor.apps.stock.service.StockMoveToolService;
import com.axelor.apps.stock.service.TrackingNumberService;
import com.axelor.apps.stock.service.WeightedAveragePriceService;
import com.axelor.apps.stock.service.app.AppStockService;
import com.axelor.apps.supplychain.db.repo.SupplychainBatchRepository;
import com.axelor.apps.supplychain.service.StockMoveLineServiceSupplychainImpl;
import com.axelor.apps.supplychain.service.app.AppSupplychainService;
import com.axelor.apps.supplychain.service.config.SupplyChainConfigService;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

public class StockMoveLineProductionServiceImpl extends StockMoveLineServiceSupplychainImpl {

  @Inject
  public StockMoveLineProductionServiceImpl(
      TrackingNumberService trackingNumberService,
      AppBaseService appBaseService,
      AppStockService appStockService,
      StockMoveToolService stockMoveToolService,
      StockMoveLineRepository stockMoveLineRepository,
      StockLocationLineService stockLocationLineService,
      UnitConversionService unitConversionService,
      WeightedAveragePriceService weightedAveragePriceService,
      TrackingNumberRepository trackingNumberRepo,
      ShippingCoefService shippingCoefService,
      AccountManagementService accountManagementService,
      PriceListService priceListService,
      ProductCompanyService productCompanyService,
      SupplychainBatchRepository supplychainBatchRepo,
      SupplyChainConfigService supplychainConfigService,
      StockLocationLineHistoryService stockLocationLineHistoryService,
      InvoiceLineRepository invoiceLineRepository,
      AppSupplychainService appSupplychainService) {
    super(
        trackingNumberService,
        appBaseService,
        appStockService,
        stockMoveToolService,
        stockMoveLineRepository,
        stockLocationLineService,
        unitConversionService,
        weightedAveragePriceService,
        trackingNumberRepo,
        shippingCoefService,
        accountManagementService,
        priceListService,
        productCompanyService,
        supplychainBatchRepo,
        supplychainConfigService,
        stockLocationLineHistoryService,
        invoiceLineRepository,
        appSupplychainService);
  }

  @Override
  @Transactional(rollbackOn = {Exception.class})
  protected void fillOriginTrackingNumber(StockMoveLine stockMoveLine) {
    super.fillOriginTrackingNumber(stockMoveLine);

    if (appBaseService.isApp("production")) {
      TrackingNumber trackingNumber = stockMoveLine.getTrackingNumber();
      if (trackingNumber != null
          && stockMoveLine.getStockMove() != null
          && stockMoveLine.getStockMove().getManufOrder() != null) {
        trackingNumber.setOriginMoveTypeSelect(
            TrackingNumberRepository.ORIGIN_MOVE_TYPE_MANUFACTURING);
        trackingNumber.setOriginManufOrder(stockMoveLine.getStockMove().getManufOrder());

        trackingNumberRepo.save(trackingNumber);
      }
    }
  }
}
