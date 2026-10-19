package sssm.dev.sssm_backend.a11_inventory.inventory_count.dto.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import sssm.dev.sssm_backend.a11_inventory.inventory_count.dto.InventoryCountResponse;
import sssm.dev.sssm_backend.a11_inventory.inventory_count.dto.rest.InventoryCountRest;

@Component
@RequiredArgsConstructor
public class InventoryCountRestMapper {

  public InventoryCountRest toRest(InventoryCountResponse rs) {
    return (new InventoryCountRest(
        rs.getId(),
        rs.getCompany_id(),
        rs.getWarehouse_id(),
        rs.getReference(),
        rs.getStatus(),
        rs.getStarted_by(),
        rs.getCompleted_by(),
        rs.getStarted_at(),
        rs.getCompleted_at(),
        rs.getNotes()));
  }
}
