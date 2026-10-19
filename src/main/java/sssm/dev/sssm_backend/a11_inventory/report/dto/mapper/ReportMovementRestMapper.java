package sssm.dev.sssm_backend.a11_inventory.report.dto.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import sssm.dev.sssm_backend.a11_inventory.report.dto.ReportMovementResponse;
import sssm.dev.sssm_backend.a11_inventory.report.dto.rest.ReportMovementRest;

@Component
@RequiredArgsConstructor
public class ReportMovementRestMapper {

  public ReportMovementRest toRest(ReportMovementResponse rs) {
    return (new ReportMovementRest(
        rs.getId(),
        rs.getWarehouse_id(),
        rs.getItem_id(),
        rs.getEmployee_id(),
        rs.getMovement_type(),
        rs.getReference(),
        rs.getMovement_date(),
        rs.getNotes(),
        rs.getSource_warehouse_id(),
        rs.getDestination_warehouse_id(),
        rs.getOperation_id(),
        rs.getQuantity(),
        rs.getPerformed_by(),
        rs.getCreated_at()));
  }
}
