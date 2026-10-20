package sssm.dev.sssm_backend.a03_authentication.user.service;

import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a03_authentication.user.entity.AppUser;
import sssm.dev.sssm_backend.a03_authentication.user.repository.*;

@Service
@RequiredArgsConstructor
public class AppUserService {
  private final AppUserDAO dao;

  public List<AppUser> findAll(Optional<Integer> page, Optional<Integer> size) {
    return dao.findAll(page.orElse(1), size.orElse(10));
  }

  public Optional<AppUser> findById(String id) {
    if (dao.findById(id).isEmpty()) {
      throw new NotFoundException("Utilisateur introuvable", "Aucun utilisateur ne correspond à l'identifiant " + id + ".", "NOT_FOUND");
    }
    return dao.findById(id);
  }

  public List<String> findRoleNames(String userId) {
    return dao.findRoleNames(userId);
  }

  public Optional<AppUser> findByUsernameOrEmail(String login) {
    if (dao.findByUsernameOrEmail(login).isEmpty()) {
      throw new NotFoundException("Utilisateur introuvable", "Aucun utilisateur ne correspond au login " + login + ".", "NOT_FOUND");
    }
    return dao.findByUsernameOrEmail(login);
  }

  public void updateLastLogin(String userId) {
    dao.updateLastLogin(userId);
  }

  public AppUser create(AppUser user) {
    return dao.create(user);
  }

  public AppUser update(String id, AppUser user) {
    if (dao.findById(id).isEmpty()) throw new NotFoundException("Utilisateur introuvable", "Aucun utilisateur ne correspond à l'identifiant " + id + ".", "NOT_FOUND");
    user.setId(id);
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
}
