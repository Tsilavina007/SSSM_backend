package sssm.dev.sssm_backend.a11_inventory.dashboard.dto.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import sssm.dev.sssm_backend.a11_inventory.dashboard.dto.DashboardLowStockResponse;
import sssm.dev.sssm_backend.a11_inventory.dashboard.dto.rest.DashboardLowStockRest;

@Component
@RequiredArgsConstructor
public class DashboardLowStockRestMapper {

  public DashboardLowStockRest toRest(DashboardLowStockResponse rs) {
    return (new DashboardLowStockRest(
        rs.getId(),
        rs.getCompany_id(),
        rs.getWarehouse_id(),
        rs.getCategory_id(),
        rs.getCode(),
        rs.getName(),
        rs.getDescription(),
        rs.getUnit(),
        rs.getQuantity_total(),
        rs.getMinimum_stock(),
        rs.getStatus(),
        rs.getCreated_at(),
        rs.getUpdated_at(),
        rs.getArchived_at(),
        rs.getStock_quantity()));
  }
}
