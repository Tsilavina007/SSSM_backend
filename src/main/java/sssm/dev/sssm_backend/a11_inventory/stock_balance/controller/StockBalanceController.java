package sssm.dev.sssm_backend.a11_inventory.stock_balance.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sssm.dev.sssm_backend.a01_common.pagination.PageResponse;
import sssm.dev.sssm_backend.a11_inventory.stock_balance.dto.rest.StockBalanceRest;
import sssm.dev.sssm_backend.a11_inventory.stock_balance.service.StockBalanceService;

@RestController
@RequestMapping("/api/v1/inventory/stock-balances")
public class StockBalanceController {
  private final StockBalanceService stockBalanceService;

  StockBalanceController(StockBalanceService stockBalanceService) {
    this.stockBalanceService = stockBalanceService;
  }

  @GetMapping
  public PageResponse<StockBalanceRest> list(
      @RequestParam(required = false) String warehouseId,
      @RequestParam(required = false) String itemTypeId,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer pageSize) {
    return stockBalanceService.list(warehouseId, itemTypeId, page, pageSize);
  }
}
