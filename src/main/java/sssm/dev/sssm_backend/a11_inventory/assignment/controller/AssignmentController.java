package sssm.dev.sssm_backend.a11_inventory.assignment.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sssm.dev.sssm_backend.a11_inventory.assignment.dto.AssignmentCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.assignment.dto.AssignmentUpdateRequest;
import sssm.dev.sssm_backend.a11_inventory.assignment.dto.rest.AssignmentRest;
import sssm.dev.sssm_backend.a11_inventory.assignment.service.AssignmentService;

@RestController
@RequestMapping("/api/inventory")
public class AssignmentController {
  private final AssignmentService assignmentService;

  AssignmentController(AssignmentService assignmentService) {
    this.assignmentService = assignmentService;
  }

  @GetMapping("/assignments")
  public List<AssignmentRest> list(
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer pageSize) {
    return assignmentService.listActive(page, pageSize);
  }

  @PostMapping("/assignments")
  public ResponseEntity<Object> create(@RequestBody AssignmentCreateRequest request) {
    return ResponseEntity.ok().body(assignmentService.save(null, request.toMap(), false));
  }

  @GetMapping("/assignments/{id}")
  public ResponseEntity<Object> get(@PathVariable String id) {
    return ResponseEntity.ok().body(assignmentService.one(id));
  }

  @PatchMapping("/assignments/{id}")
  public ResponseEntity<Object> patch(
      @PathVariable String id, @RequestBody AssignmentUpdateRequest request) {
    return ResponseEntity.ok().body(assignmentService.save(id, request.toMap(), true));
  }

  @PostMapping("/assignments/{id}/return")
  public ResponseEntity<Object> ret(
      @PathVariable String id, @RequestBody AssignmentUpdateRequest request) {
    return ResponseEntity.ok().body(assignmentService.save(id, request.toMap(), true));
  }

  @GetMapping("/employees/{id}/assignments")
  public List<AssignmentRest> employee(@PathVariable String id) {
    return assignmentService.listByEmployee(id);
  }

  @GetMapping("/security-posts/{id}/assignments")
  public List<AssignmentRest> post(@PathVariable String id) {
    return assignmentService.listByPost(id);
  }
}
