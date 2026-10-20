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
import sssm.dev.sssm_backend.a03_authentication.user.dto.AppUserCreateRequest;
import sssm.dev.sssm_backend.a03_authentication.user.dto.AppUserUpdateRequest;
import sssm.dev.sssm_backend.a03_authentication.user.entity.AppUser;
import sssm.dev.sssm_backend.a03_authentication.user.repository.AppUserDAO;

@Service
@RequiredArgsConstructor
public class AppUserService {
  private final AppUserDAO dao;
  private final PasswordEncoder passwordEncoder;

  public List<AppUser> findAll(Optional<Integer> page, Optional<Integer> size) {
    return dao.findAll(page.orElse(1), size.orElse(10));
  }

  public AppUser findById(String id) {
    return dao.findById(id)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Utilisateur introuvable",
                    "Aucun utilisateur ne correspond à l'identifiant " + id + ".",
                    "NOT_FOUND"));
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

  public AppUser create(AppUserCreateRequest request) {
    String username = request.getUsername().trim();
    String email = normalizeEmail(request.getEmail());

    ensureUsernameAvailable(username);
    ensureEmailAvailable(email, null);

    AppUser user = new AppUser();
    user.setCompany_id(request.getCompany_id());
    user.setUsername(username);
    user.setEmail(email);
    user.setPassword_hash(passwordEncoder.encode(request.getPassword()));
    user.setFirst_name(request.getFirst_name());
    user.setLast_name(request.getLast_name());
    user.setPhone(request.getPhone());
    user.setStatus(request.getStatus() == null ? UserStatus.ACTIVE : request.getStatus());
    return dao.create(user);
  }

  public AppUser update(String id, AppUserUpdateRequest request) {
    findById(id);

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
    return dao.update(user);
  }

  public void delete(String id) {
    dao.delete(id);
  }

  public void assignRole(String userId, String roleId) {
    dao.assignRole(userId, roleId);
  }

  public void removeRole(String userId, String roleId) {
    dao.removeRole(userId, roleId);
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
}
