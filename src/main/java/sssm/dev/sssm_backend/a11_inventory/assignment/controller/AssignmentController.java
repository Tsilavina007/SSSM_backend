package sssm.dev.sssm_backend.a11_inventory.assignment.controller;

import java.util.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sssm.dev.sssm_backend.a11_inventory.assignment.service.AssignmentService;

@RestController
@RequestMapping("/api/inventory")
public class AssignmentController {
  private final AssignmentService service = new AssignmentService();

  @GetMapping("/assignments")
  public Object list(
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer pageSize) {
    return service.list(page, pageSize, null, "returned_at IS NULL");
  }

  @PostMapping("/assignments")
  public Object create(@RequestBody Map<String, Object> b) {
    return service.save(null, b, false);
  }

  @GetMapping("/assignments/{id}")
  public Object get(@PathVariable String id) {
    return service.one(id);
  }

  @PatchMapping("/assignments/{id}")
  public Object patch(@PathVariable String id, @RequestBody Map<String, Object> b) {
    return service.save(id, b, true);
  }

  @PostMapping("/assignments/{id}/return")
  public Object ret(@PathVariable String id, @RequestBody Map<String, Object> b) {
    return service.save(id, b, true);
  }

  @GetMapping("/employees/{id}/assignments")
  public Object employee(@PathVariable String id) {
    return service.list(1, 500, null, "employee_id=?", id);
  }

  @GetMapping("/security-posts/{id}/assignments")
  public Object post(@PathVariable String id) {
    return service.list(1, 500, null, "post_id=?", id);
  }
}
