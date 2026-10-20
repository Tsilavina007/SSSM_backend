package sssm.dev.sssm_backend.a03_authentication.role.controller;

import java.util.*;
import org.springframework.web.bind.annotation.*;

import sssm.dev.sssm_backend.a03_authentication.role.entity.Role;
import sssm.dev.sssm_backend.a03_authentication.role.service.RoleService;

@RestController
@RequestMapping("/api/roles")
public class RoleController {
  private final RoleService service;

  RoleController(RoleService service) {
    this.service = service;
  }

  @GetMapping
  public List<Role> findAll(
      @RequestParam Optional<Integer> page, @RequestParam Optional<Integer> pageSize) {
    return service.findAll(page, pageSize);
  }

  @GetMapping("/{id}")
  public Optional<Role> findById(@PathVariable String id) {
    return service.findById(id);
  }

  @PostMapping
  public Role create(@RequestBody Role role) {
    return service.create(role);
  }

  @PatchMapping("/{id}")
  public Role update(@PathVariable String id, @RequestBody Role role) {
    return service.update(id, role);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable String id) {
    service.delete(id);
  }

  @PostMapping("/{roleId}/permissions/{permissionId}")
  public void assignPermission(@PathVariable String roleId, @PathVariable String permissionId) {
    service.assignPermission(roleId, permissionId);
  }

  @DeleteMapping("/{roleId}/permissions/{permissionId}")
  public void removePermission(@PathVariable String roleId, @PathVariable String permissionId) {
    service.removePermission(roleId, permissionId);
  }
}
