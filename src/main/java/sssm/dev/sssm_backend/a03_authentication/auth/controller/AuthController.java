package sssm.dev.sssm_backend.a03_authentication.auth.controller;

import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import sssm.dev.sssm_backend.a03_authentication.auth.dto.*;
import sssm.dev.sssm_backend.a03_authentication.auth.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthService service;

  AuthController(AuthService service) {
    this.service = service;
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
    return service.register(request);
  }

  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest request) {
    return service.login(request);
  }

  @GetMapping("/whoami")
  public AuthResponse.UserResponse whoAmI(Authentication authentication) {
    return service.whoAmI(authentication.getName());
  }

  @PostMapping("/refresh")
  public AuthResponse refresh(@RequestBody Map<String, String> body) {
    String token = body.get("token");
    if (token == null || token.isBlank()) {
      throw new sssm.dev.sssm_backend.a00_infrastructure.exception.ClientException(
          "Token manquant",
          "Le champ 'token' est obligatoire.",
          "CLIENT_ERROR");
    }
    return service.refresh(token);
  }
}
