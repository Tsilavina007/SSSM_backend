package sssm.dev.sssm_backend.a11_inventory.item_assignment.repository;

import java.util.List;
import java.util.Optional;

import sssm.dev.sssm_backend.a11_inventory.item_assignment.entity.ItemAssignment;

public interface ItemAssignmentDAO {
  List<ItemAssignment> getAllItemAssignments(int page, int pageSize);

  List<ItemAssignment> searchItemAssignmentsByName(String value, int page, int pageSize);

  Optional<ItemAssignment> getItemAssignmentById(String id);

  ItemAssignment createItemAssignment(ItemAssignment value);

  ItemAssignment updateItemAssignment(ItemAssignment value);

  void deleteItemAssignment(String id);
}
