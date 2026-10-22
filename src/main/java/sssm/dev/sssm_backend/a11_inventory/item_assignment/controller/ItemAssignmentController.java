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
      @RequestParam Optional<String> search,
      @RequestParam Optional<Boolean> active) {
    return itemAssignmentService.getItemAssignments(
        search, page, pageSize, active.orElse(false));
  }

  @GetMapping("/{itemAssignmentId}")
  public ItemAssignmentRest getItemAssignmentById(@PathVariable String itemAssignmentId) {
    return itemAssignmentService.getItemAssignmentById(itemAssignmentId);
  }

  @GetMapping("/item/{itemId}")
  public List<ItemAssignmentRest> getItemAssignmentsByItem(@PathVariable String itemId) {
    return itemAssignmentService.getItemAssignmentsByItem(itemId, 1, 500);
  }

  @GetMapping("/employee/{employeeId}")
  public List<ItemAssignmentRest> getItemAssignmentsByEmployee(@PathVariable String employeeId) {
    return itemAssignmentService.getItemAssignmentsByEmployee(employeeId, 1, 500);
  }

  @GetMapping("/post/{postId}")
  public List<ItemAssignmentRest> getItemAssignmentsByPost(@PathVariable String postId) {
    return itemAssignmentService.getItemAssignmentsByPost(postId, 1, 500);
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

  @PostMapping("/{itemAssignmentId}/return")
  public ItemAssignmentRest returnItemAssignment(
      @PathVariable String itemAssignmentId,
      @RequestBody(required = false) ItemAssignmentUpdateRequest value) {
    return itemAssignmentService.returnItemAssignment(itemAssignmentId, value);
  }

  @PostMapping("/{itemAssignmentId}/void")
  public ItemAssignmentRest voidItemAssignment(
      @PathVariable String itemAssignmentId, @RequestParam String reason) {
    return itemAssignmentService.voidItemAssignment(itemAssignmentId, reason);
  }

  @PostMapping("/{itemAssignmentId}/restore")
  public ItemAssignmentRest restoreItemAssignment(@PathVariable String itemAssignmentId) {
    return itemAssignmentService.restoreItemAssignment(itemAssignmentId);
  }

  @DeleteMapping("/{itemAssignmentId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteItemAssignment(
      @PathVariable String itemAssignmentId,
      @RequestParam(required = false) String reason) {
    itemAssignmentService.deleteItemAssignment(itemAssignmentId, reason);
  }
}
