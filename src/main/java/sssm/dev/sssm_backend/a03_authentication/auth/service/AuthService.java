package sssm.dev.sssm_backend.a03_authentication.auth.service;

import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a00_infrastructure.exception.ClientException;
import sssm.dev.sssm_backend.a01_common.enums.UserStatus;
import sssm.dev.sssm_backend.a03_authentication.auth.dto.*;
import sssm.dev.sssm_backend.a03_authentication.role.repository.*;
import sssm.dev.sssm_backend.a03_authentication.user.entity.AppUser;
import sssm.dev.sssm_backend.a03_authentication.user.repository.AppUserDAOImpl;

@Service
public class AuthService {
  private final AppUserDAOImpl users = new AppUserDAOImpl();
  private final RoleDAO roles = new RoleDAOImpl();
  private final PasswordEncoder encoder;
  private final JwtService jwt;
  private final String defaultRole;

  public AuthService(
      PasswordEncoder encoder,
      JwtService jwt,
      @Value("${app.auth.default-role:USER}") String defaultRole) {
    this.encoder = encoder;
    this.jwt = jwt;
    this.defaultRole = defaultRole;
  }

  public AuthResponse register(RegisterRequest request) {
    if (request == null) {
      throw new ClientException("Request body is missing");
    }
    if (request.email() == null) {
      throw new ClientException("Request body is missing, email required");
    }
    if (request.username() == null) {
      throw new ClientException("Request body is missing, username required");
    }
    if (users.findByUsernameOrEmail(request.username()).isPresent()
        || users.findByUsernameOrEmail(request.email()).isPresent()) {
      throw new ClientException("Username or Email already exists");
    }
    AppUser user = new AppUser();
    user.setUsername(request.username().trim());
    user.setEmail(request.email() == null ? null : request.email().trim().toLowerCase(Locale.ROOT));
    user.setPassword_hash(encoder.encode(request.password()));
    user.setFirst_name(request.firstName());
    user.setLast_name(request.lastName());
    user.setPhone(request.phone());
    user.setStatus(UserStatus.ACTIVE);
    user = users.create(user);
    String userId = user.getId();
    roles.findGlobalByName(defaultRole).ifPresent(role -> users.assignRole(userId, role.getId()));
    List<String> roleNames = users.findRoleNames(user.getId());
    return registerResponse(user, roleNames);
  }

  public AuthResponse login(LoginRequest request) {
    AppUser user =
        users
            .findByUsernameOrEmail(request.login())
            .filter(u -> u.getStatus() == UserStatus.ACTIVE)
            .orElseThrow(() -> new ClientException("Invalid credentials or inactive user account"));
    if (!encoder.matches(request.password(), user.getPassword_hash()))
      throw new ClientException("Invalid credentials or inactive user account");
    users.updateLastLogin(user.getId());
    List<String> roleNames = users.findRoleNames(user.getId());
    return new AuthResponse(
        jwt.createToken(user.getId(), user.getUsername(), roleNames),
        "Bearer",
        toResponse(user, roleNames));
  }

  private AuthResponse registerResponse(AppUser user, List<String> roles) {
    return new AuthResponse(null, "Bearer", toResponse(user, roles));
  }

  private AuthResponse.UserResponse toResponse(AppUser u, List<String> roles) {
    return new AuthResponse.UserResponse(
        u.getId(), u.getUsername(), u.getEmail(), u.getFirst_name(), u.getLast_name(), roles);
  }
}
