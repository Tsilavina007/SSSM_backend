package sssm.dev.sssm_backend.a03_authentication.auth.dto;

import java.util.List;

public record AuthResponse(String token, String tokenType, UserResponse user) {
  public record UserResponse(
      String id, String username, String email, String firstName, String lastName, List<String> roles) {}
}
