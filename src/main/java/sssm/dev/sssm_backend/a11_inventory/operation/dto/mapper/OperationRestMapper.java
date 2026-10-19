package sssm.dev.sssm_backend.a11_inventory.operation.dto.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import sssm.dev.sssm_backend.a11_inventory.operation.dto.OperationResponse;
import sssm.dev.sssm_backend.a11_inventory.operation.dto.rest.OperationRest;

@Component
@RequiredArgsConstructor
public class OperationRestMapper {

  public OperationRest toRest(OperationResponse rs) {
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
        rs.getCompleted_at()));
  }
}
