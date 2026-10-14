package sssm.dev.sssm_backend.a03_authentication.auth.controller;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import sssm.dev.sssm_backend.a00_infrastructure.exception.ClientException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;
import sssm.dev.sssm_backend.a01_common.errors.ItemError;
import sssm.dev.sssm_backend.a01_common.errors.mapper.ErrorMapper;
import sssm.dev.sssm_backend.a03_authentication.auth.dto.*;
import sssm.dev.sssm_backend.a03_authentication.auth.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  @Autowired private AuthService service;

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<Object> register(
      @Valid @RequestBody RegisterRequest request, BindingResult bindingResult) {
    if (request == null) {
      return ResponseEntity.badRequest().body(new ItemError("Request body is missing", null, null));
    }
    if (bindingResult.hasErrors()) {
      Map<String, ItemError> errors = new HashMap<>();
      for (FieldError error : bindingResult.getFieldErrors()) {
        errors.put(error.getField(), new ItemError(error.getDefaultMessage(), null, null));
      }
      return ResponseEntity.badRequest().body(Map.of("errors", errors));
    }

    try {
      AuthResponse response = service.register(request);
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (ClientException e) {
      return ResponseEntity.badRequest().body(Map.of("errors", ErrorMapper.toItemError(e)));
    } catch (NotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("errors", ErrorMapper.toItemError(e)));
    } catch (ServerException e) {
      return ResponseEntity.internalServerError()
          .body(Map.of("errors", ErrorMapper.toItemError(e)));
    }
  }

  @PostMapping("/login")
  public ResponseEntity<Object> login(@Valid @RequestBody LoginRequest request) {
    try {
      AuthResponse response = service.login(request);
      return ResponseEntity.ok().body(response);
    } catch (ClientException e) {
      return ResponseEntity.badRequest().body(Map.of("errors", ErrorMapper.toItemError(e)));
    } catch (NotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("errors", ErrorMapper.toItemError(e)));
    } catch (ServerException e) {
      return ResponseEntity.internalServerError()
          .body(Map.of("errors", ErrorMapper.toItemError(e)));
    }
  }
}
