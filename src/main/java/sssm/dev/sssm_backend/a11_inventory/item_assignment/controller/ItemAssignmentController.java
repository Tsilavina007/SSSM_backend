package sssm.dev.sssm_backend.a11_inventory.item_assignment.controller;

import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
  public ResponseEntity<Object> getItemAssignments(
      @RequestParam Optional<Integer> page,
      @RequestParam Optional<Integer> pageSize,
      @RequestParam Optional<String> search) {
    List<ItemAssignmentRest> itemAssignments =
        itemAssignmentService.getItemAssignments(search, page, pageSize);
    return ResponseEntity.ok().body(itemAssignments);
  }

  @GetMapping("/{itemAssignmentId}")
  public ResponseEntity<Object> getItemAssignmentById(@PathVariable String itemAssignmentId) {
    Optional<ItemAssignmentRest> itemAssignment =
        itemAssignmentService.getItemAssignmentById(itemAssignmentId);
    return ResponseEntity.ok().body(itemAssignment);
  }

  @PostMapping
  public ResponseEntity<Object> createItemAssignment(
      @RequestBody ItemAssignmentCreateRequest itemAssignment) {
    ItemAssignmentRest itemAssignmentRes =
        itemAssignmentService.createItemAssignment(itemAssignment);
    return ResponseEntity.ok().body(itemAssignmentRes);
  }

  @PatchMapping("/{itemAssignmentId}")
  public ResponseEntity<Object> updateItemAssignment(
      @PathVariable String itemAssignmentId,
      @RequestBody ItemAssignmentUpdateRequest updatedItemAssignment) {
    ItemAssignmentRest itemAssignmentRes =
        itemAssignmentService.updateItemAssignment(itemAssignmentId, updatedItemAssignment);
    return ResponseEntity.ok().body(itemAssignmentRes);
  }

  @DeleteMapping("/{itemAssignmentId}")
  public ResponseEntity<Object> deleteItemAssignment(@PathVariable String itemAssignmentId) {
    itemAssignmentService.deleteItemAssignment(itemAssignmentId);
    return ResponseEntity.ok().build();
  }
}
