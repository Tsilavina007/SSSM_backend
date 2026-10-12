package sssm.dev.sssm_backend.a11_inventory.item_assignment.service;

import java.util.List;
import java.util.Optional;

import sssm.dev.sssm_backend.a11_inventory.item_assignment.entity.ItemAssignment;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.repository.ItemAssignmentDAO;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.repository.ItemAssignmentDAOImpl;

public class ItemAssignmentService {
  private final ItemAssignmentDAO itemAssignmentDAO;

  public ItemAssignmentService() {
    this.itemAssignmentDAO = new ItemAssignmentDAOImpl();
  }

  public List<ItemAssignment> getItemAssignments(
      Optional<String> search, Optional<Integer> page, Optional<Integer> pageSize) {
    int pageNumber = page.orElse(1);
    int pageSizeNumber = pageSize.orElse(10);
    return search.isPresent()
        ? itemAssignmentDAO.searchItemAssignmentsByName(search.get(), pageNumber, pageSizeNumber)
        : itemAssignmentDAO.getAllItemAssignments(pageNumber, pageSizeNumber);
  }

  public Optional<ItemAssignment> getItemAssignmentById(String itemAssignmentId) {
    return itemAssignmentDAO.getItemAssignmentById(itemAssignmentId);
  }

  public ItemAssignment createItemAssignment(ItemAssignment itemAssignment) {
    return itemAssignmentDAO.createItemAssignment(itemAssignment);
  }

  public ItemAssignment updateItemAssignment(
      String itemAssignmentId, ItemAssignment updatedItemAssignment) {
    if (itemAssignmentDAO.getItemAssignmentById(itemAssignmentId).isEmpty()) {
      throw new IllegalArgumentException("ItemAssignment not found with ID: " + itemAssignmentId);
    }
    updatedItemAssignment.setId(itemAssignmentId);
    return itemAssignmentDAO.updateItemAssignment(updatedItemAssignment);
  }

  public void deleteItemAssignment(String itemAssignmentId) {
    itemAssignmentDAO.deleteItemAssignment(itemAssignmentId);
  }
}
