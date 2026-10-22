package sssm.dev.sssm_backend.a11_inventory.item_assignment.dto.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import sssm.dev.sssm_backend.a11_inventory.item_assignment.dto.ItemAssignmentResponse;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.dto.rest.ItemAssignmentRest;

@Component
@RequiredArgsConstructor
public class ItemAssignmentRestMapper {

  public ItemAssignmentRest toRest(ItemAssignmentResponse rs) {
    return new ItemAssignmentRest(
        rs.getId(),
        rs.getCompany_id(),
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
        rs.getExpected_return_at(),
        rs.getSource_warehouse_id(),
        rs.getVoided_at(),
        rs.getVoided_by(),
        rs.getVoid_reason());
  }
}
