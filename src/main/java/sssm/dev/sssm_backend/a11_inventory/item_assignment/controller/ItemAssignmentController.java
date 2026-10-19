package sssm.dev.sssm_backend.a11_inventory.item_assignment.controller;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import java.util.List;
import java.util.Map;
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

import sssm.dev.sssm_backend.a00_infrastructure.exception.ClientException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;
import sssm.dev.sssm_backend.a01_common.errors.mapper.ErrorMapper;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.dto.ItemAssignmentCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.dto.ItemAssignmentUpdateRequest;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.entity.ItemAssignment;
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
    try {
      List<ItemAssignment> itemAssignments =
          itemAssignmentService.getItemAssignments(search, page, pageSize);
      return ResponseEntity.ok().body(itemAssignments);
    } catch (ClientException e) {
      return ResponseEntity.badRequest().body(Map.of("errors", ErrorMapper.toItemError(e)));
    } catch (NotFoundException e) {
      return ResponseEntity.status(NOT_FOUND).body(Map.of("errors", ErrorMapper.toItemError(e)));
    } catch (ServerException e) {
      return ResponseEntity.internalServerError()
          .body(Map.of("errors", ErrorMapper.toItemError(e)));
    }
  }

  @GetMapping("/{itemAssignmentId}")
  public ResponseEntity<Object> getItemAssignmentById(@PathVariable String itemAssignmentId) {
    try {
      Optional<ItemAssignment> itemAssignment =
          itemAssignmentService.getItemAssignmentById(itemAssignmentId);
      return ResponseEntity.ok().body(itemAssignment);
    } catch (ClientException e) {
      return ResponseEntity.badRequest().body(Map.of("errors", ErrorMapper.toItemError(e)));
    } catch (NotFoundException e) {
      return ResponseEntity.status(NOT_FOUND).body(Map.of("errors", ErrorMapper.toItemError(e)));
    } catch (ServerException e) {
      return ResponseEntity.internalServerError()
          .body(Map.of("errors", ErrorMapper.toItemError(e)));
    }
  }

  @PostMapping
  public ResponseEntity<Object> createItemAssignment(
      @RequestBody ItemAssignmentCreateRequest itemAssignment) {
    try {
      ItemAssignment itemAssignmentRes = itemAssignmentService.createItemAssignment(itemAssignment);
      return ResponseEntity.ok().body(itemAssignmentRes);
    } catch (ClientException e) {
      return ResponseEntity.badRequest().body(Map.of("errors", ErrorMapper.toItemError(e)));
    } catch (NotFoundException e) {
      return ResponseEntity.status(NOT_FOUND).body(Map.of("errors", ErrorMapper.toItemError(e)));
    } catch (ServerException e) {
      return ResponseEntity.internalServerError()
          .body(Map.of("errors", ErrorMapper.toItemError(e)));
    }
  }

  @PatchMapping("/{itemAssignmentId}")
  public ResponseEntity<Object> updateItemAssignment(
      @PathVariable String itemAssignmentId,
      @RequestBody ItemAssignmentUpdateRequest updatedItemAssignment) {
    try {
      ItemAssignment itemAssignmentRes =
          itemAssignmentService.updateItemAssignment(itemAssignmentId, updatedItemAssignment);
      return ResponseEntity.ok().body(itemAssignmentRes);
    } catch (ClientException e) {
      return ResponseEntity.badRequest().body(Map.of("errors", ErrorMapper.toItemError(e)));
    } catch (NotFoundException e) {
      return ResponseEntity.status(NOT_FOUND).body(Map.of("errors", ErrorMapper.toItemError(e)));
    } catch (ServerException e) {
      return ResponseEntity.internalServerError()
          .body(Map.of("errors", ErrorMapper.toItemError(e)));
    }
  }

  @DeleteMapping("/{itemAssignmentId}")
  public ResponseEntity<Object> deleteItemAssignment(@PathVariable String itemAssignmentId) {
    try {
      itemAssignmentService.deleteItemAssignment(itemAssignmentId);
      return ResponseEntity.ok().build();
    } catch (ClientException e) {
      return ResponseEntity.badRequest().body(Map.of("errors", ErrorMapper.toItemError(e)));
    } catch (NotFoundException e) {
      return ResponseEntity.status(NOT_FOUND).body(Map.of("errors", ErrorMapper.toItemError(e)));
    } catch (ServerException e) {
      return ResponseEntity.internalServerError()
          .body(Map.of("errors", ErrorMapper.toItemError(e)));
    }
  }
}
