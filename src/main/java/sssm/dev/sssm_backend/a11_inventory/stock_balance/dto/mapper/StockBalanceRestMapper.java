package sssm.dev.sssm_backend.a11_inventory.stock_balance.dto.mapper;

import org.springframework.stereotype.Component;

import sssm.dev.sssm_backend.a11_inventory.stock_balance.dto.StockBalanceResponse;
import sssm.dev.sssm_backend.a11_inventory.stock_balance.dto.rest.StockBalanceRest;

@Component
public class StockBalanceRestMapper {

  public StockBalanceRest toRest(StockBalanceResponse value) {
    return new StockBalanceRest(
        value.getId(),
        value.getCompany_id(),
        value.getWarehouse_id(),
        value.getWarehouse_name(),
        value.getItem_type_id(),
        value.getItem_type_code(),
        value.getItem_type_name(),
        value.getQuantity(),
        value.getMinimum_stock(),
        value.isBelow_minimum(),
        value.getVersion(),
        value.getUpdated_at());
  }
}
