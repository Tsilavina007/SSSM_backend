package sssm.dev.sssm_backend.a03_authentication.role.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import sssm.dev.sssm_backend.a03_authentication.role.dto.RoleCreateRequest;
import sssm.dev.sssm_backend.a03_authentication.role.dto.RoleResponse;
import sssm.dev.sssm_backend.a03_authentication.role.dto.RoleUpdateRequest;
import sssm.dev.sssm_backend.a03_authentication.role.service.RoleService;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {
  private final RoleService service;

  RoleController(RoleService service) {
    this.service = service;
  }

  @GetMapping
  public List<RoleResponse> findAll(
      @RequestParam Optional<Integer> page, @RequestParam Optional<Integer> pageSize) {
    return service.findAll(page, pageSize);
  }

  @GetMapping("/{id}")
  public RoleResponse findById(@PathVariable String id) {
    return service.findById(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public RoleResponse create(@Valid @RequestBody RoleCreateRequest request) {
    return service.create(request);
  }

  @PatchMapping("/{id}")
  public RoleResponse update(
      @PathVariable String id, @Valid @RequestBody RoleUpdateRequest request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable String id) {
    service.delete(id);
  }

  @PostMapping("/{roleId}/permissions/{permissionId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void assignPermission(@PathVariable String roleId, @PathVariable String permissionId) {
    service.assignPermission(roleId, permissionId);
  }

  @DeleteMapping("/{roleId}/permissions/{permissionId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removePermission(@PathVariable String roleId, @PathVariable String permissionId) {
    service.removePermission(roleId, permissionId);
  }
}
