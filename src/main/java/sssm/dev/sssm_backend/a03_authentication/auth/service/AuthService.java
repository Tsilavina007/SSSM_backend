package sssm.dev.sssm_backend.a03_authentication.auth.service;

import io.jsonwebtoken.Claims;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a00_infrastructure.exception.ClientException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.UnauthorizedException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ForbiddenException;
import sssm.dev.sssm_backend.a01_common.enums.UserStatus;
import sssm.dev.sssm_backend.a03_authentication.auth.dto.*;
import sssm.dev.sssm_backend.a03_authentication.auth.dto.AuthResponse.UserResponse;
import sssm.dev.sssm_backend.a03_authentication.auth.repository.*;
import sssm.dev.sssm_backend.a03_authentication.auth.security.CurrentUser;
import sssm.dev.sssm_backend.a03_authentication.auth.security.JwtPrincipal;
import sssm.dev.sssm_backend.a03_authentication.role.repository.*;
import sssm.dev.sssm_backend.a03_authentication.user.entity.AppUser;
import sssm.dev.sssm_backend.a03_authentication.user.repository.AppUserDAO;

@Service
public class AuthService {
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final AppUserDAO users;
  private final RoleDAO roles;
  private final PasswordEncoder encoder;
  private final JwtService jwt;
  private final RefreshTokenDAO refreshTokens;
  private final RevokedAccessTokenDAO revokedTokens;
  private final CurrentUser currentUser;
  private final String defaultRole;
  private final long refreshExpirationMs;

  public AuthService(
      AppUserDAO users,
      RoleDAO roles,
      PasswordEncoder encoder,
      JwtService jwt,
      RefreshTokenDAO refreshTokens,
      RevokedAccessTokenDAO revokedTokens,
      CurrentUser currentUser,
      @Value("${app.auth.default-role:USER}") String defaultRole,
      @Value("${app.jwt.refresh-expiration-ms:2592000000}") long refreshExpirationMs) {
    this.users = users;
    this.roles = roles;
    this.encoder = encoder;
    this.jwt = jwt;
    this.refreshTokens = refreshTokens;
    this.revokedTokens = revokedTokens;
    this.currentUser = currentUser;
    this.defaultRole = defaultRole;
    this.refreshExpirationMs = refreshExpirationMs;
  }

  /** Inscription réservée aux administrateurs. Un administrateur d'entreprise ne peut créer que dans
   * sa propre entreprise ; un administrateur plateforme utilise /api/users pour choisir l'entreprise.
   */
  public AuthResponse register(RegisterRequest request) {
    if (request == null) {
      throw new ClientException(
          "Corps de requête manquant", "Le corps de la requête est obligatoire.", "CLIENT_ERROR");
    }
    if (request.email() == null) {
      throw new ClientException(
          "Email obligatoire", "Le champ 'email' est obligatoire.", "CLIENT_ERROR");
    }
    if (request.username() == null) {
      throw new ClientException(
          "Nom d'utilisateur obligatoire", "Le champ 'username' est obligatoire.", "CLIENT_ERROR");
    }

    JwtPrincipal principal = currentUser.require();
    String companyId = principal.companyId();
    if (companyId == null || companyId.isBlank()) {
      throw new ClientException(
          "Entreprise obligatoire",
          "L'administrateur doit être rattaché à une entreprise. Utilisez /api/users pour créer un "
              + "compte plateforme.",
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
    user.setCompany_id(companyId);
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
            .orElseThrow(
                () ->
                    new UnauthorizedException(
                        "Identifiants invalides",
                        "Aucun compte ne correspond à cet identifiant."));

    if (user.getStatus() != UserStatus.ACTIVE) {
      throw new ForbiddenException(
          "Compte inactif",
          "Votre compte n'est pas actif. Contactez l'administration.");
    }

    if (!encoder.matches(request.password(), user.getPassword_hash())) {
      throw new UnauthorizedException(
          "Identifiants invalides",
          "Le mot de passe est incorrect.");
    }

    users.updateLastLogin(user.getId());
    return authenticatedResponse(user);
  }

  public AuthResponse refresh(String rawRefreshToken) {
    if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
      throw new UnauthorizedException(
          "Refresh token manquant", "Le refresh token est obligatoire.");
    }

    RefreshTokenRecord stored =
        refreshTokens
            .consumeByHash(hash(rawRefreshToken))
            .orElseThrow(
                () ->
                    new UnauthorizedException(
                        "Refresh token invalide",
                        "Le refresh token est inconnu, expiré ou a déjà été utilisé."));

    AppUser user =
        users
            .findById(stored.userId())
            .orElseThrow(
                () ->
                    new UnauthorizedException(
                        "Refresh token invalide",
                        "L'utilisateur associé au refresh token n'existe plus."));

    if (user.getStatus() != UserStatus.ACTIVE) {
      throw new UnauthorizedException("Compte inactif", "Le compte utilisateur n'est pas actif.");
    }

    List<String> roleNames = users.findRoleNames(user.getId());
    List<String> permissions = users.findPermissionCodes(user.getId());
    boolean platformAdmin = users.isPlatformAdmin(user.getId());
    String accessToken =
        jwt.createAccessToken(
            user.getId(),
            user.getUsername(),
            user.getCompany_id(),
            roleNames,
            permissions,
            platformAdmin);

    IssuedRefreshToken replacement = issueRefreshToken(user.getId());
    refreshTokens.markReplaced(stored.id(), replacement.id());

    return new AuthResponse(
        accessToken, replacement.rawToken(), "Bearer", toResponse(user, roleNames));
  }

  /** Révoque le refresh token fourni et, si possible, l'access token courant. */
  public void logout(String rawRefreshToken, String accessToken) {
    if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
      refreshTokens.revokeByHash(hash(rawRefreshToken));
    }
    if (accessToken == null || accessToken.isBlank()) return;
    try {
      Claims claims = jwt.parse(accessToken);
      String jti = claims.getId();
      if (jti != null) {
        revokedTokens.revoke(jti, claims.get("uid", String.class), claims.getExpiration().toInstant());
      }
    } catch (RuntimeException ignored) {
      // Logout idempotent : un access token déjà expiré ou illisible ne doit pas empêcher
      // la révocation du refresh token.
    }
  }

  public UserResponse whoAmI(String username) {
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

  private AuthResponse authenticatedResponse(AppUser user) {
    List<String> roleNames = users.findRoleNames(user.getId());
    List<String> permissions = users.findPermissionCodes(user.getId());
    boolean platformAdmin = users.isPlatformAdmin(user.getId());
    String accessToken =
        jwt.createAccessToken(
            user.getId(),
            user.getUsername(),
            user.getCompany_id(),
            roleNames,
            permissions,
            platformAdmin);
    IssuedRefreshToken refreshToken = issueRefreshToken(user.getId());
    return new AuthResponse(
        accessToken, refreshToken.rawToken(), "Bearer", toResponse(user, roleNames));
  }

  private IssuedRefreshToken issueRefreshToken(String userId) {
    byte[] bytes = new byte[32];
    SECURE_RANDOM.nextBytes(bytes);
    String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    String id =
        refreshTokens.create(
            userId, hash(rawToken), Instant.now().plusMillis(refreshExpirationMs), null, null);
    return new IssuedRefreshToken(id, rawToken);
  }

  private static String hash(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of()
          .formatHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 indisponible", e);
    }
  }

  private AuthResponse registerResponse(AppUser user, List<String> roles) {
    return new AuthResponse(null, "Bearer", toResponse(user, roles));
  }

  private AuthResponse.UserResponse toResponse(AppUser u, List<String> roles) {
    return new AuthResponse.UserResponse(
        u.getId(), u.getUsername(), u.getEmail(), u.getFirst_name(), u.getLast_name(), roles, u.getCompany_id());
  }

  private record IssuedRefreshToken(String id, String rawToken) {}
}
