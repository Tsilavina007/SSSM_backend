package sssm.dev.sssm_backend.a11_inventory.item_assignment.repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import sssm.dev.sssm_backend.a11_inventory.item_assignment.dto.ItemAssignmentCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.dto.ItemAssignmentResponse;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.dto.ItemAssignmentUpdateRequest;

public interface ItemAssignmentDAO {
  List<ItemAssignmentResponse> getAllItemAssignments(int page, int pageSize, boolean activeOnly);

  List<ItemAssignmentResponse> searchItemAssignmentsByName(
      String value, int page, int pageSize, boolean activeOnly);

  List<ItemAssignmentResponse> getItemAssignmentsByItem(String itemId, int page, int pageSize);

  List<ItemAssignmentResponse> getItemAssignmentsByEmployee(
      String employeeId, int page, int pageSize);

  List<ItemAssignmentResponse> getItemAssignmentsByPost(String postId, int page, int pageSize);

  long countItemAssignments(String search, boolean activeOnly);

  long countItemAssignmentsByItem(String itemId);

  List<ItemAssignmentResponse> getItemAssignmentsByItem(
      String itemId, int page, int pageSize, boolean activeOnly);

  long countItemAssignmentsByItem(String itemId, boolean activeOnly);

  long countItemAssignmentsByEmployee(String employeeId);

  long countItemAssignmentsByPost(String postId);

  Optional<ItemAssignmentResponse> getItemAssignmentById(String id);

  Optional<ItemAssignmentResponse> getActiveItemAssignmentByItem(String itemId);

  ItemAssignmentResponse createItemAssignment(
      ItemAssignmentCreateRequest value,
      String companyId,
      String sourceWarehouseId,
      String assignedBy);

  ItemAssignmentResponse updateItemAssignmentMetadata(String id, ItemAssignmentUpdateRequest value);

  int markReturned(String id, Timestamp returnedAt, String returnedBy, String conditionOnReturn);

  int voidActiveItemAssignment(String id, Timestamp voidedAt, String voidedBy, String reason);

  int archiveInactiveItemAssignment(String id, Timestamp voidedAt, String voidedBy, String reason);

  void restoreItemAssignment(String id);
}
