package sssm.dev.sssm_backend.a11_inventory.item_assignment.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.dto.ItemAssignmentCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.dto.ItemAssignmentUpdateRequest;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.entity.ItemAssignment;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.repository.ItemAssignmentDAO;

@Service
@RequiredArgsConstructor
public class ItemAssignmentService {
  private final ItemAssignmentDAO itemAssignmentDAO;

  public List<ItemAssignment> getItemAssignments(
      Optional<String> search, Optional<Integer> page, Optional<Integer> pageSize) {
    int pageNumber = page.orElse(1);
    int pageSizeNumber = pageSize.orElse(10);
    return search.isPresent()
        ? itemAssignmentDAO.searchItemAssignmentsByName(search.get(), pageNumber, pageSizeNumber)
        : itemAssignmentDAO.getAllItemAssignments(pageNumber, pageSizeNumber);
  }

  public Optional<ItemAssignment> getItemAssignmentById(String itemAssignmentId) {
    Optional<ItemAssignment> itemAssignment =
        itemAssignmentDAO.getItemAssignmentById(itemAssignmentId);
    if (itemAssignment.isEmpty()) {
      throw new NotFoundException("ItemAssignment not found with ID: " + itemAssignmentId);
    }
    return Optional.of(itemAssignment.get());
  }

  public ItemAssignment createItemAssignment(ItemAssignmentCreateRequest itemAssignment) {
    return itemAssignmentDAO.createItemAssignment(itemAssignment);
  }

  public ItemAssignment updateItemAssignment(
      String itemAssignmentId, ItemAssignmentUpdateRequest updatedItemAssignment) {
    if (itemAssignmentDAO.getItemAssignmentById(itemAssignmentId).isEmpty()) {
      throw new NotFoundException("ItemAssignment not found with ID: " + itemAssignmentId);
    }
    return itemAssignmentDAO.updateItemAssignment(itemAssignmentId, updatedItemAssignment);
  }

  public void deleteItemAssignment(String itemAssignmentId) {
    itemAssignmentDAO.deleteItemAssignment(itemAssignmentId);
  }
}
