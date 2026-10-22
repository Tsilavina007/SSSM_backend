package sssm.dev.sssm_backend.a11_inventory.inventory_count.dto.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import sssm.dev.sssm_backend.a11_inventory.inventory_count.dto.InventoryCountScanResponse;
import sssm.dev.sssm_backend.a11_inventory.inventory_count.dto.rest.InventoryCountScanRest;

@Component
@RequiredArgsConstructor
public class InventoryCountScanRestMapper {

  public InventoryCountScanRest toRest(InventoryCountScanResponse rs) {
    return (new InventoryCountScanRest(
        rs.getId(),
        rs.getInventory_count_id(),
        rs.getItem_id(),
        rs.getScanned_by(),
        rs.getScanned_at(),
        rs.getExpected_warehouse_id(),
        rs.getActual_warehouse_id(),
        rs.getExpected_quantity(),
        rs.getCounted_quantity(),
        rs.getDifference(),
        rs.getDiscrepancy_type(),
        rs.getNotes(),
        rs.getQr_code()));
  }
}
