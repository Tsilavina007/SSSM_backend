package sssm.dev.sssm_backend.a03_authentication.user.controller;

import java.util.*;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import sssm.dev.sssm_backend.a00_infrastructure.exception.ClientException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;
import sssm.dev.sssm_backend.a03_authentication.user.entity.AppUser;
import sssm.dev.sssm_backend.a03_authentication.user.service.AppUserService;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/users")
public class AppUserController {
  private final AppUserService service = new AppUserService();

  @GetMapping
  public ResponseEntity<Object> findAll(
      @RequestParam Optional<Integer> page, @RequestParam Optional<Integer> pageSize) {
    try {
      List<AppUser> users = service.findAll(page, pageSize);
      return ResponseEntity.ok().body(users);
    } catch (ClientException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    } catch (NotFoundException e) {
      return ResponseEntity.status(NOT_FOUND).body(e.getMessage());
    } catch (ServerException e) {
      return ResponseEntity.internalServerError().body(e.getMessage());
    }
  }

  @GetMapping("/{id}")
  public ResponseEntity<Object> findById(@PathVariable String id) {
    try {
      Optional<AppUser> user = service.findById(id);
      return ResponseEntity.ok().body(user.orElseThrow());
    } catch (ClientException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    } catch (NotFoundException e) {
      return ResponseEntity.status(NOT_FOUND).body(e.getMessage());
    } catch (ServerException e) {
      return ResponseEntity.internalServerError().body(e.getMessage());
    }
  }

  @PostMapping
  public ResponseEntity<Object> create(@RequestBody AppUser user) {
    try {
      AppUser createdUser = service.create(user);
      return ResponseEntity.ok().body(createdUser);
    } catch (ClientException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    } catch (ServerException e) {
      return ResponseEntity.internalServerError().body(e.getMessage());
    }
  }

  @PatchMapping("/{id}")
  public ResponseEntity<Object> update(@PathVariable String id, @RequestBody AppUser user) {
    try {
      AppUser updatedUser = service.update(id, user);
      return ResponseEntity.ok().body(updatedUser);
    } catch (ClientException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    } catch (NotFoundException e) {
      return ResponseEntity.status(NOT_FOUND).body(e.getMessage());
    } catch (ServerException e) {
      return ResponseEntity.internalServerError().body(e.getMessage());
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Object> delete(@PathVariable String id) {
    try {
      service.delete(id);
      return ResponseEntity.ok().build();
    } catch (ClientException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    } catch (NotFoundException e) {
      return ResponseEntity.status(NOT_FOUND).body(e.getMessage());
    } catch (ServerException e) {
      return ResponseEntity.internalServerError().body(e.getMessage());
    }
  }

  @PostMapping("/{userId}/roles/{roleId}")
  public ResponseEntity<Object> assignRole(@PathVariable String userId, @PathVariable String roleId) {
    try {
      service.assignRole(userId, roleId);
      return ResponseEntity.ok().build();
    } catch (ClientException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    } catch (ServerException e) {
      return ResponseEntity.internalServerError().body(e.getMessage());
    }
  }

  @DeleteMapping("/{userId}/roles/{roleId}")
  public ResponseEntity<Object> removeRole(@PathVariable String userId, @PathVariable String roleId) {
    try {
      service.removeRole(userId, roleId);
      return ResponseEntity.ok().build();
    } catch (ClientException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    } catch (ServerException e) {
      return ResponseEntity.internalServerError().body(e.getMessage());
    }
  }
}
