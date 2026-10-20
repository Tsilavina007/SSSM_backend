package sssm.dev.sssm_backend.a03_authentication.user.controller;

import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import sssm.dev.sssm_backend.a03_authentication.user.dto.AppUserCreateRequest;
import sssm.dev.sssm_backend.a03_authentication.user.dto.AppUserUpdateRequest;
import sssm.dev.sssm_backend.a03_authentication.user.entity.AppUser;
import sssm.dev.sssm_backend.a03_authentication.user.service.AppUserService;

@RestController
@RequestMapping("/api/users")
public class AppUserController {
  private final AppUserService service;

  AppUserController(AppUserService service) {
    this.service = service;
  }

  @GetMapping
  public ResponseEntity<Object> findAll(
      @RequestParam Optional<Integer> page, @RequestParam Optional<Integer> pageSize) {
    List<AppUser> users = service.findAll(page, pageSize);
    return ResponseEntity.ok().body(users);
  }

  @GetMapping("/{id}")
  public ResponseEntity<Object> findById(@PathVariable String id) {
    AppUser user = service.findById(id);
    return ResponseEntity.ok().body(user);
  }

  @PostMapping
  public ResponseEntity<Object> create(@Valid @RequestBody AppUserCreateRequest request) {
    AppUser createdUser = service.create(request);
    return ResponseEntity.ok().body(createdUser);
  }

  @PatchMapping("/{id}")
  public ResponseEntity<Object> update(
      @PathVariable String id, @Valid @RequestBody AppUserUpdateRequest request) {
    AppUser updatedUser = service.update(id, request);
    return ResponseEntity.ok().body(updatedUser);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Object> delete(@PathVariable String id) {
    service.delete(id);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/{userId}/roles/{roleId}")
  public ResponseEntity<Object> assignRole(
      @PathVariable String userId, @PathVariable String roleId) {
    service.assignRole(userId, roleId);
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{userId}/roles/{roleId}")
  public ResponseEntity<Object> removeRole(
      @PathVariable String userId, @PathVariable String roleId) {
    service.removeRole(userId, roleId);
    return ResponseEntity.ok().build();
  }
}
