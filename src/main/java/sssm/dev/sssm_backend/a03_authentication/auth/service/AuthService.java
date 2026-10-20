package sssm.dev.sssm_backend.a03_authentication.auth.service;

import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a00_infrastructure.exception.ClientException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
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
      throw new ClientException(
          "Corps de requête manquant",
          "Le corps de la requête est obligatoire.",
          "CLIENT_ERROR");
    }
    if (request.email() == null) {
      throw new ClientException(
          "Email obligatoire", "Le champ 'email' est obligatoire.", "CLIENT_ERROR");
    }
    if (request.username() == null) {
      throw new ClientException(
          "Nom d'utilisateur obligatoire",
          "Le champ 'username' est obligatoire.",
          "CLIENT_ERROR");
    }
    if (users.findByUsernameOrEmail(request.username()).isPresent()
        || users.findByUsernameOrEmail(request.email()).isPresent()) {
      throw new ClientException(
          "Nom d'utilisateur ou email déjà utilisé",
          "Un compte existe déjà avec ce nom d'utilisateur ou cette adresse email.",
          "CLIENT_ERROR");
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
            .orElseThrow(
                () ->
                    new ClientException(
                        "Identifiants invalides",
                        "Le login/email ou le mot de passe est incorrect, ou le compte n'est pas actif.",
                        "CLIENT_ERROR"));
    if (!encoder.matches(request.password(), user.getPassword_hash()))
      throw new ClientException(
          "Identifiants invalides",
          "Le login/email ou le mot de passe est incorrect, ou le compte n'est pas actif.",
          "CLIENT_ERROR");
    users.updateLastLogin(user.getId());
    List<String> roleNames = users.findRoleNames(user.getId());
    return new AuthResponse(
        jwt.createToken(user.getId(), user.getUsername(), roleNames),
        "Bearer",
        toResponse(user, roleNames));
  }

  public AuthResponse.UserResponse whoAmI(String username) {
    AppUser user =
        users
            .findByUsername(username)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Utilisateur authentifié introuvable",
                        "Aucun utilisateur ne correspond au nom d'utilisateur '"
                            + username
                            + "' contenu dans le jeton.",
                        "NOT_FOUND"));
    return toResponse(user, users.findRoleNames(user.getId()));
  }

  private AuthResponse registerResponse(AppUser user, List<String> roles) {
    return new AuthResponse(null, "Bearer", toResponse(user, roles));
  }

  private AuthResponse.UserResponse toResponse(AppUser u, List<String> roles) {
    return new AuthResponse.UserResponse(
        u.getId(), u.getUsername(), u.getEmail(), u.getFirst_name(), u.getLast_name(), roles);
  }
}
