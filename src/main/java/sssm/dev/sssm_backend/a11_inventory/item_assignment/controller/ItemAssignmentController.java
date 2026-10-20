package sssm.dev.sssm_backend.a11_inventory.item_assignment.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import sssm.dev.sssm_backend.a11_inventory.item_assignment.dto.ItemAssignmentCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.dto.ItemAssignmentUpdateRequest;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.dto.rest.ItemAssignmentRest;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.service.ItemAssignmentService;

@RestController
@RequestMapping("/api/item-assignments")
public class ItemAssignmentController {
  private final ItemAssignmentService itemAssignmentService;

  ItemAssignmentController(ItemAssignmentService itemAssignmentService) {
    this.itemAssignmentService = itemAssignmentService;
  }

  @GetMapping
  public List<ItemAssignmentRest> getItemAssignments(
      @RequestParam Optional<Integer> page,
      @RequestParam Optional<Integer> pageSize,
      @RequestParam Optional<String> search) {
    return itemAssignmentService.getItemAssignments(search, page, pageSize);
  }

  @GetMapping("/{itemAssignmentId}")
  public ItemAssignmentRest getItemAssignmentById(@PathVariable String itemAssignmentId) {
    return itemAssignmentService.getItemAssignmentById(itemAssignmentId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ItemAssignmentRest createItemAssignment(
      @Valid @RequestBody ItemAssignmentCreateRequest itemAssignment) {
    return itemAssignmentService.createItemAssignment(itemAssignment);
  }

  @PatchMapping("/{itemAssignmentId}")
  public ItemAssignmentRest updateItemAssignment(
      @PathVariable String itemAssignmentId,
      @Valid @RequestBody ItemAssignmentUpdateRequest updatedItemAssignment) {
    return itemAssignmentService.updateItemAssignment(itemAssignmentId, updatedItemAssignment);
  }

  @DeleteMapping("/{itemAssignmentId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteItemAssignment(@PathVariable String itemAssignmentId) {
    itemAssignmentService.deleteItemAssignment(itemAssignmentId);
  }
}
