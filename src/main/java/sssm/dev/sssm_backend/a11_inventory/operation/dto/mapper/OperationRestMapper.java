package sssm.dev.sssm_backend.a11_inventory.operation.dto.mapper;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import sssm.dev.sssm_backend.a11_inventory.operation.dto.OperationLineResponse;
import sssm.dev.sssm_backend.a11_inventory.operation.dto.OperationResponse;
import sssm.dev.sssm_backend.a11_inventory.operation.dto.rest.OperationLineRest;
import sssm.dev.sssm_backend.a11_inventory.operation.dto.rest.OperationRest;

@Component
@RequiredArgsConstructor
public class OperationRestMapper {

  public OperationRest toRest(OperationResponse rs) {
    List<OperationLineRest> lines =
        rs.getLines() == null ? List.of() : rs.getLines().stream().map(this::lineToRest).toList();
    return (new OperationRest(
        rs.getId(),
        rs.getCompany_id(),
        rs.getOperation_type(),
        rs.getStatus(),
        rs.getReference(),
        rs.getIdempotency_key(),
        rs.getSource_warehouse_id(),
        rs.getDestination_warehouse_id(),
        rs.getEmployee_id(),
        rs.getPost_id(),
        rs.getPerformed_by(),
        rs.getNotes(),
        rs.getCreated_at(),
        rs.getCompleted_at(),
        lines));
  }

  private OperationLineRest lineToRest(OperationLineResponse rs) {
    return new OperationLineRest(
        rs.getId(),
        rs.getOperation_id(),
        rs.getItem_id(),
        rs.getMovement_id(),
        rs.getQuantity(),
        rs.getCondition_before(),
        rs.getCondition_after(),
        rs.getScanned_at(),
        rs.getNotes());
  }
}
