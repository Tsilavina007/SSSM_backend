package sssm.dev.sssm_backend.a03_authentication.permission.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import sssm.dev.sssm_backend.a03_authentication.permission.dto.PermissionCreateRequest;
import sssm.dev.sssm_backend.a03_authentication.permission.dto.PermissionResponse;
import sssm.dev.sssm_backend.a03_authentication.permission.dto.PermissionUpdateRequest;
import sssm.dev.sssm_backend.a03_authentication.permission.service.PermissionService;

@RestController
@RequestMapping("/api/v1/permissions")
public class PermissionController {
  private final PermissionService service;

  PermissionController(PermissionService service) {
    this.service = service;
  }

  @GetMapping
  public List<PermissionResponse> findAll(
      @RequestParam Optional<Integer> page, @RequestParam Optional<Integer> pageSize) {
    return service.findAll(page, pageSize);
  }

  @GetMapping("/{id}")
  public PermissionResponse findById(@PathVariable String id) {
    return service.findById(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PermissionResponse create(@Valid @RequestBody PermissionCreateRequest request) {
    return service.create(request);
  }

  @PatchMapping("/{id}")
  public PermissionResponse update(
      @PathVariable String id, @Valid @RequestBody PermissionUpdateRequest request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable String id) {
    service.delete(id);
  }
}
