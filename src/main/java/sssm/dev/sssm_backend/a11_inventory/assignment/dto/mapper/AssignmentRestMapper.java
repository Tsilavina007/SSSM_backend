package sssm.dev.sssm_backend.a11_inventory.assignment.dto.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import sssm.dev.sssm_backend.a11_inventory.assignment.dto.AssignmentResponse;
import sssm.dev.sssm_backend.a11_inventory.assignment.dto.rest.AssignmentRest;

@Component
@RequiredArgsConstructor
public class AssignmentRestMapper {

  public AssignmentRest toRest(AssignmentResponse rs) {
    return (new AssignmentRest(
        rs.getId(),
        rs.getItem_id(),
        rs.getEmployee_id(),
        rs.getPost_id(),
        rs.getAssigned_at(),
        rs.getReturned_at(),
        rs.getCondition_on_assignment(),
        rs.getCondition_on_return(),
        rs.getNotes(),
        rs.getCreated_at(),
        rs.getUpdated_at(),
        rs.getAssigned_by(),
        rs.getReturned_by(),
        rs.getExpected_return_at()));
  }
}
