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
    String token = body == null ? null : body.get("refreshToken");
    if (token == null || token.isBlank()) {
      // Compatibilité de transition : l'ancien champ token est accepté, mais il doit contenir
      // un refresh token et non un access token.
      token = body == null ? null : body.get("token");
    }
    if (token == null || token.isBlank()) {
      throw new sssm.dev.sssm_backend.a00_infrastructure.exception.ClientException(
          "Refresh token manquant", "Le champ 'refreshToken' est obligatoire.", "CLIENT_ERROR");
    }
    return service.refresh(token);
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(
      @RequestBody(required = false) Map<String, String> body,
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    String refreshToken = body == null ? null : body.get("refreshToken");
    if (refreshToken == null || refreshToken.isBlank()) {
      refreshToken = body == null ? null : body.get("token");
    }
    String accessToken =
        authorization != null && authorization.startsWith("Bearer ")
            ? authorization.substring(7)
            : null;
    service.logout(refreshToken, accessToken);
  }
}
