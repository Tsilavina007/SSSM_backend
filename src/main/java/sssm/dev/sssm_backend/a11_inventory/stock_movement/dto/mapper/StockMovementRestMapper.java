package sssm.dev.sssm_backend.a11_inventory.stock_movement.dto.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import sssm.dev.sssm_backend.a11_inventory.stock_movement.dto.StockMovementResponse;
import sssm.dev.sssm_backend.a11_inventory.stock_movement.dto.rest.StockMovementRest;

@Component
@RequiredArgsConstructor
public class StockMovementRestMapper {

  public StockMovementRest toRest(StockMovementResponse rs) {
    return (new StockMovementRest(
        rs.getId(),
        rs.getWarehouse_id(),
        rs.getItem_id(),
        rs.getEmployee_id(),
        rs.getMovement_type(),
        rs.getReference(),
        rs.getMovement_date(),
        rs.getNotes()));
  }
}
