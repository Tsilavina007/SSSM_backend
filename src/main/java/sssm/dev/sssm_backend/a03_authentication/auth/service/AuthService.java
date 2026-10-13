package sssm.dev.sssm_backend.a03_authentication.auth.service;

import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a00_infrastructure.exception.ClientException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;
import sssm.dev.sssm_backend.a01_common.enums.UserStatus;
import sssm.dev.sssm_backend.a03_authentication.auth.dto.*;
import sssm.dev.sssm_backend.a03_authentication.role.repository.*;
import sssm.dev.sssm_backend.a03_authentication.user.entity.AppUser;
import sssm.dev.sssm_backend.a03_authentication.user.service.AppUserService;

@Service
public class AuthService {
  private final AppUserService users = new AppUserService();
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
    try {
      users.findByUsernameOrEmail(request.username());
      throw new ClientException("Username already exists: " + request.username());
    } catch (NotFoundException e) {
      if (request.email() != null) {
        try {
          users.findByUsernameOrEmail(request.email());
          throw new ClientException("Email already exists: " + request.email());
        } catch (NotFoundException e2) {
          // No user found with the given username or email, proceed with registration
        }
      }
    }
    AppUser user = new AppUser();
    user.setUsername(request.username().trim());
    user.setEmail(request.email() == null ? null : request.email().trim().toLowerCase(Locale.ROOT));
    user.setPassword_hash(encoder.encode(request.password()));
    user.setFirst_name(request.firstName());
    user.setLast_name(request.lastName());
    user.setPhone(request.phone());
    user.setStatus(UserStatus.ACTIVE);
    try { 
      user = users.create(user);
    } catch (ServerException e) {
      throw new ServerException("Failed to create user: " + e.getMessage());
    } catch (ClientException e) {
      throw new ClientException("Failed to create user: " + e.getMessage());
    } catch (Exception e) {
      throw new ServerException("An unexpected error occurred while creating the user: " + e.getMessage());
    }
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
