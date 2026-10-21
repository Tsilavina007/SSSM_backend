package sssm.dev.sssm_backend.a03_authentication.user.service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ClientException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a01_common.enums.UserStatus;
import sssm.dev.sssm_backend.a03_authentication.auth.security.CurrentUser;
import sssm.dev.sssm_backend.a03_authentication.role.entity.Role;
import sssm.dev.sssm_backend.a03_authentication.role.repository.RoleDAO;
import sssm.dev.sssm_backend.a03_authentication.user.dto.AppUserCreateRequest;
import sssm.dev.sssm_backend.a03_authentication.user.dto.AppUserResponse;
import sssm.dev.sssm_backend.a03_authentication.user.dto.AppUserUpdateRequest;
import sssm.dev.sssm_backend.a03_authentication.user.entity.AppUser;
import sssm.dev.sssm_backend.a03_authentication.user.repository.AppUserDAO;

@Service
@RequiredArgsConstructor
public class AppUserService {
  private final AppUserDAO dao;
  private final RoleDAO roles;
  private final PasswordEncoder passwordEncoder;
  private final CurrentUser currentUser;

  public List<AppUserResponse> findAll(Optional<Integer> page, Optional<Integer> size) {
    int requestedPage = page.orElse(1);
    int requestedSize = size.orElse(10);
    List<AppUser> users =
        currentUser.isPlatformAdmin()
            ? dao.findAll(requestedPage, requestedSize)
            : dao.findAllByCompany(currentUser.requireCompanyId(), requestedPage, requestedSize);
    return users.stream().map(this::toResponse).toList();
  }

  public AppUserResponse findById(String id) {
    return toResponse(requireAccessibleUser(id));
  }

  public List<String> findRoleNames(String userId) {
    return dao.findRoleNames(userId);
  }

  public Optional<AppUser> findByUsernameOrEmail(String login) {
    Optional<AppUser> user = dao.findByUsernameOrEmail(login);
    if (user.isEmpty()) {
      throw new NotFoundException(
          "Utilisateur introuvable",
          "Aucun utilisateur ne correspond au login " + login + ".",
          "NOT_FOUND");
    }
    return user;
  }

  public void updateLastLogin(String userId) {
    dao.updateLastLogin(userId);
  }

  public AppUserResponse create(AppUserCreateRequest request) {
    String username = request.getUsername().trim();
    String email = normalizeEmail(request.getEmail());

    ensureUsernameAvailable(username);
    ensureEmailAvailable(email, null);

    AppUser user = new AppUser();
    user.setCompany_id(resolveCompanyForCreate(request.getCompany_id()));
    user.setUsername(username);
    user.setEmail(email);
    user.setPassword_hash(passwordEncoder.encode(request.getPassword()));
    user.setFirst_name(request.getFirst_name());
    user.setLast_name(request.getLast_name());
    user.setPhone(request.getPhone());
    user.setStatus(request.getStatus() == null ? UserStatus.ACTIVE : request.getStatus());
    return toResponse(dao.create(user));
  }

  public AppUserResponse update(String id, AppUserUpdateRequest request) {
    requireAccessibleUser(id);

    String email = normalizeEmail(request.getEmail());
    if (email != null) {
      ensureEmailAvailable(email, id);
    }

    AppUser user = new AppUser();
    user.setId(id);
    if (email != null) {
      user.setEmail(email);
    }
    if (request.getPassword() != null) {
      user.setPassword_hash(passwordEncoder.encode(request.getPassword()));
    }
    user.setFirst_name(request.getFirst_name());
    user.setLast_name(request.getLast_name());
    user.setPhone(request.getPhone());
    user.setStatus(request.getStatus());
    return toResponse(dao.update(user));
  }

  public void delete(String id) {
    requireAccessibleUser(id);
    dao.delete(id);
  }

  public void assignRole(String userId, String roleId) {
    requireAccessibleUser(userId);
    requireAssignableRole(roleId);
    dao.assignRole(userId, roleId);
  }

  public void removeRole(String userId, String roleId) {
    requireAccessibleUser(userId);
    requireAssignableRole(roleId);
    dao.removeRole(userId, roleId);
  }

  private String resolveCompanyForCreate(String requestedCompanyId) {
    if (currentUser.isPlatformAdmin()) {
      return requestedCompanyId;
    }
    String companyId = currentUser.requireCompanyId();
    if (requestedCompanyId != null && !requestedCompanyId.equals(companyId)) {
      throw new ClientException(
          "Entreprise non autorisée",
          "Un administrateur d'entreprise ne peut créer que des comptes dans sa propre entreprise.",
          "CLIENT_ERROR");
    }
    return companyId;
  }

  private AppUser requireAccessibleUser(String id) {
    AppUser user =
        dao.findById(id)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Utilisateur introuvable",
                        "Aucun utilisateur ne correspond à l'identifiant " + id + ".",
                        "NOT_FOUND"));
    if (currentUser.isPlatformAdmin()) return user;

    String companyId = currentUser.requireCompanyId();
    if (!companyId.equals(user.getCompany_id())) {
      throw new NotFoundException(
          "Utilisateur introuvable",
          "Aucun utilisateur ne correspond à l'identifiant " + id + ".",
          "NOT_FOUND");
    }
    return user;
  }

  private Role requireAssignableRole(String roleId) {
    Role role =
        roles
            .findById(roleId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Rôle introuvable",
                        "Aucun rôle ne correspond à l'identifiant " + roleId + ".",
                        "NOT_FOUND"));
    if (currentUser.isPlatformAdmin()) return role;

    String companyId = currentUser.requireCompanyId();
    if (!companyId.equals(role.getCompany_id())) {
      throw new NotFoundException(
          "Rôle introuvable",
          "Aucun rôle ne correspond à l'identifiant " + roleId + ".",
          "NOT_FOUND");
    }
    return role;
  }

  private void ensureUsernameAvailable(String username) {
    if (dao.findByUsername(username).isPresent()) {
      throw new ClientException(
          "Nom d'utilisateur déjà utilisé",
          "Le nom d'utilisateur '" + username + "' est déjà attribué à un autre compte.",
          "CLIENT_ERROR");
    }
  }

  private void ensureEmailAvailable(String email, String ignoredUserId) {
    dao.findByUsernameOrEmail(email)
        .filter(existing -> !existing.getId().equals(ignoredUserId))
        .ifPresent(
            existing -> {
              throw new ClientException(
                  "Adresse email déjà utilisée",
                  "L'adresse email '" + email + "' est déjà attribuée à un autre compte.",
                  "CLIENT_ERROR");
            });
  }

  private static String normalizeEmail(String email) {
    return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
  }

  private AppUserResponse toResponse(AppUser user) {
    return new AppUserResponse(
        user.getId(),
        user.getCompany_id(),
        user.getUsername(),
        user.getEmail(),
        user.getFirst_name(),
        user.getLast_name(),
        user.getPhone(),
        user.getStatus(),
        user.getLast_login_at(),
        user.getCreated_at(),
        user.getUpdated_at());
  }
}
