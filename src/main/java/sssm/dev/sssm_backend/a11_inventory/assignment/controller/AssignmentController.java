package sssm.dev.sssm_backend.a11_inventory.assignment.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
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

/**
 * Façade de compatibilité de l'ancien module d'affectations.
 *
 * <p>Les routes restent disponibles pendant la période de dépréciation, mais elles délèguent au
 * contrat canonique {@code /api/item-assignments} : mêmes validations, mêmes codes HTTP et même
 * cycle de vie. Le JSON libre et le soft-200 ont été supprimés.
 */
@Deprecated
@RestController
@RequestMapping("/api/v1/inventory")
public class AssignmentController {
  private static final String DEPRECATION_HEADER = "true";
  private static final String SUCCESSOR_LINK =
      "</api/v1/item-assignments>; rel=\"successor-version\"";

  private final ItemAssignmentService itemAssignmentService;

  AssignmentController(ItemAssignmentService itemAssignmentService) {
    this.itemAssignmentService = itemAssignmentService;
  }

  @GetMapping("/assignments")
  public ResponseEntity<List<ItemAssignmentRest>> list(
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer pageSize,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) Boolean active) {
    return deprecated(
        HttpStatus.OK,
        itemAssignmentService
            .getItemAssignments(
                Optional.ofNullable(search),
                Optional.ofNullable(page),
                Optional.ofNullable(pageSize),
                Boolean.TRUE.equals(active))
            .items());
  }

  @PostMapping("/assignments")
  public ResponseEntity<ItemAssignmentRest> create(
      @Valid @RequestBody ItemAssignmentCreateRequest request) {
    return deprecated(HttpStatus.CREATED, itemAssignmentService.createItemAssignment(request));
  }

  @GetMapping("/assignments/{id}")
  public ResponseEntity<ItemAssignmentRest> get(@PathVariable String id) {
    return deprecated(HttpStatus.OK, itemAssignmentService.getItemAssignmentById(id));
  }

  @PatchMapping("/assignments/{id}")
  public ResponseEntity<ItemAssignmentRest> patch(
      @PathVariable String id, @Valid @RequestBody ItemAssignmentUpdateRequest request) {
    return deprecated(HttpStatus.OK, itemAssignmentService.updateItemAssignment(id, request));
  }

  @PostMapping("/assignments/{id}/return")
  public ResponseEntity<ItemAssignmentRest> ret(
      @PathVariable String id, @RequestBody(required = false) ItemAssignmentUpdateRequest request) {
    return deprecated(HttpStatus.OK, itemAssignmentService.returnItemAssignment(id, request));
  }

  @PostMapping("/assignments/{id}/void")
  public ResponseEntity<ItemAssignmentRest> voidAssignment(
      @PathVariable String id, @RequestParam String reason) {
    return deprecated(HttpStatus.OK, itemAssignmentService.voidItemAssignment(id, reason));
  }

  @PostMapping("/assignments/{id}/restore")
  public ResponseEntity<ItemAssignmentRest> restore(@PathVariable String id) {
    return deprecated(HttpStatus.OK, itemAssignmentService.restoreItemAssignment(id));
  }

  @DeleteMapping("/assignments/{id}")
  public ResponseEntity<Void> delete(
      @PathVariable String id, @RequestParam(required = false) String reason) {
    itemAssignmentService.deleteItemAssignment(id, reason);
    return ResponseEntity.noContent()
        .header("Deprecation", DEPRECATION_HEADER)
        .header("Link", SUCCESSOR_LINK)
        .build();
  }

  @GetMapping("/employees/{id}/assignments")
  public ResponseEntity<List<ItemAssignmentRest>> employee(@PathVariable String id) {
    return deprecated(
        HttpStatus.OK,
        itemAssignmentService.getItemAssignmentsByEmployee(id, 1, 500).items());
  }

  @GetMapping("/security-posts/{id}/assignments")
  public ResponseEntity<List<ItemAssignmentRest>> post(@PathVariable String id) {
    return deprecated(
        HttpStatus.OK, itemAssignmentService.getItemAssignmentsByPost(id, 1, 500).items());
  }

  @GetMapping("/items/{id}/assignments")
  public ResponseEntity<List<ItemAssignmentRest>> item(@PathVariable String id) {
    return deprecated(
        HttpStatus.OK, itemAssignmentService.getItemAssignmentsByItem(id, 1, 500).items());
  }

  private <T> ResponseEntity<T> deprecated(HttpStatus status, T body) {
    return ResponseEntity.status(status)
        .header("Deprecation", DEPRECATION_HEADER)
        .header("Link", SUCCESSOR_LINK)
        .body(body);
  }
}
