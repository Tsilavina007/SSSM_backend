package sssm.dev.sssm_backend.a03_authentication.auth.dto;

import java.util.List;

public record AuthResponse(
    String token, String refreshToken, String tokenType, UserResponse user) {

  /** Constructeur de compatibilité pour les réponses sans refresh token. */
  public AuthResponse(String token, String tokenType, UserResponse user) {
    this(token, null, tokenType, user);
  }

  public record UserResponse(
      String id,
      String username,
      String email,
      String firstName,
      String lastName,
      List<String> roles,
      String companyId) {}
}
