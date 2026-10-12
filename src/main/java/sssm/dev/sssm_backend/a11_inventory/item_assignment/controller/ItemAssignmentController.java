package sssm.dev.sssm_backend.a11_inventory.item_assignment.controller;

import java.util.List;
import java.util.Optional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sssm.dev.sssm_backend.a11_inventory.item_assignment.entity.ItemAssignment;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.service.ItemAssignmentService;

@RestController
@RequestMapping("/api/item-assignments")
public class ItemAssignmentController {
  private final ItemAssignmentService itemAssignmentService;

  public ItemAssignmentController() {
    this.itemAssignmentService = new ItemAssignmentService();
  }

  @GetMapping
  public List<ItemAssignment> getItemAssignments(
      @RequestParam Optional<Integer> page,
      @RequestParam Optional<Integer> pageSize,
      @RequestParam Optional<String> search) {
    return itemAssignmentService.getItemAssignments(search, page, pageSize);
  }

  @GetMapping("/{itemAssignmentId}")
  public Optional<ItemAssignment> getItemAssignmentById(@PathVariable String itemAssignmentId) {
    return itemAssignmentService.getItemAssignmentById(itemAssignmentId);
  }

  @PostMapping
  public ItemAssignment createItemAssignment(@RequestBody ItemAssignment itemAssignment) {
    return itemAssignmentService.createItemAssignment(itemAssignment);
  }

  @PatchMapping("/{itemAssignmentId}")
  public ItemAssignment updateItemAssignment(
      @PathVariable String itemAssignmentId, @RequestBody ItemAssignment updatedItemAssignment) {
    return itemAssignmentService.updateItemAssignment(itemAssignmentId, updatedItemAssignment);
  }

  @DeleteMapping("/{itemAssignmentId}")
  public void deleteItemAssignment(@PathVariable String itemAssignmentId) {
    itemAssignmentService.deleteItemAssignment(itemAssignmentId);
  }
}
