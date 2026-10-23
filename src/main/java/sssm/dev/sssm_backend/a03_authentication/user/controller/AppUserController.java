package sssm.dev.sssm_backend.a03_authentication.user.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import sssm.dev.sssm_backend.a03_authentication.user.dto.AppUserCreateRequest;
import sssm.dev.sssm_backend.a03_authentication.user.dto.AppUserResponse;
import sssm.dev.sssm_backend.a03_authentication.user.dto.AppUserUpdateRequest;
import sssm.dev.sssm_backend.a03_authentication.user.service.AppUserService;

@RestController
@RequestMapping("/api/v1/users")
public class AppUserController {
  private final AppUserService service;

  AppUserController(AppUserService service) {
    this.service = service;
  }

  @GetMapping
  public List<AppUserResponse> findAll(
      @RequestParam Optional<Integer> page, @RequestParam Optional<Integer> pageSize) {
    return service.findAll(page, pageSize);
  }

  @GetMapping("/{id}")
  public AppUserResponse findById(@PathVariable String id) {
    return service.findById(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public AppUserResponse create(@Valid @RequestBody AppUserCreateRequest request) {
    return service.create(request);
  }

  @PatchMapping("/{id}")
  public AppUserResponse update(
      @PathVariable String id, @Valid @RequestBody AppUserUpdateRequest request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable String id) {
    service.delete(id);
  }

  @PostMapping("/{userId}/roles/{roleId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void assignRole(@PathVariable String userId, @PathVariable String roleId) {
    service.assignRole(userId, roleId);
  }

  @DeleteMapping("/{userId}/roles/{roleId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removeRole(@PathVariable String userId, @PathVariable String roleId) {
    service.removeRole(userId, roleId);
  }
}
