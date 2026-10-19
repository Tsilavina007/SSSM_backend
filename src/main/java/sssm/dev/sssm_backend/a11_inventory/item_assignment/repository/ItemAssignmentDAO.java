package sssm.dev.sssm_backend.a11_inventory.item_assignment.repository;

import java.util.List;
import java.util.Optional;

import sssm.dev.sssm_backend.a11_inventory.item_assignment.dto.ItemAssignmentCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.dto.ItemAssignmentResponse;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.dto.ItemAssignmentUpdateRequest;

public interface ItemAssignmentDAO {
  List<ItemAssignmentResponse> getAllItemAssignments(int page, int pageSize);

  List<ItemAssignmentResponse> searchItemAssignmentsByName(String value, int page, int pageSize);

  Optional<ItemAssignmentResponse> getItemAssignmentById(String id);

  ItemAssignmentResponse createItemAssignment(ItemAssignmentCreateRequest value);

  ItemAssignmentResponse updateItemAssignment(String id, ItemAssignmentUpdateRequest value);

  void deleteItemAssignment(String id);
}
