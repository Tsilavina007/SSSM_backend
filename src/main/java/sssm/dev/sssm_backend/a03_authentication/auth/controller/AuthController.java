package sssm.dev.sssm_backend.a03_authentication.auth.controller;

import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import sssm.dev.sssm_backend.a00_infrastructure.exception.ClientException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;
import sssm.dev.sssm_backend.a03_authentication.auth.dto.*;
import sssm.dev.sssm_backend.a03_authentication.auth.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthService service;

  public AuthController(AuthService service) {
    this.service = service;
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<Object> register(@Valid @RequestBody RegisterRequest request ,
        BindingResult bindingResult) {
    if (request == null) {
      return ResponseEntity.badRequest().body("Request body is missing");
    }
    if (bindingResult.hasErrors()) {
        Map<String, String> errors = new HashMap<>();

        for (FieldError error : bindingResult.getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        return ResponseEntity.badRequest().body(errors);
    }

    try {
      AuthResponse response = service.register(request);
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (ClientException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    } catch (NotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    } catch (ServerException e) {
      return ResponseEntity.internalServerError().body(e.getMessage());
    }
  }

  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest request) {
    try {
      return service.login(request);
    } catch (ClientException e) {
      throw new ClientException(e.getMessage());
    } catch (NotFoundException e) {
      throw new NotFoundException(e.getMessage());
    } catch (ServerException e) {
      throw new ServerException(e.getMessage());
    }
  }
}
