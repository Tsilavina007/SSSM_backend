package sssm.dev.sssm_backend.a11_inventory.stock_balance.repository;

import java.math.BigDecimal;
import java.util.List;

import sssm.dev.sssm_backend.a11_inventory.stock_balance.dto.StockBalanceResponse;

/** Accès transactionnel au solde de stock calculé. */
public interface StockBalanceDAO {
  BigDecimal adjust(String companyId, String warehouseId, String itemTypeId, BigDecimal delta);

  BigDecimal quantity(String companyId, String warehouseId, String itemTypeId);

  List<StockBalanceResponse> list(
      String warehouseId, String itemTypeId, Integer page, Integer pageSize);

  long count(String warehouseId, String itemTypeId);
}
