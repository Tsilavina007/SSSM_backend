package sssm.dev.sssm_backend.a03_authentication.user.service;

import java.util.*;
import sssm.dev.sssm_backend.a03_authentication.user.entity.AppUser;
import sssm.dev.sssm_backend.a03_authentication.user.repository.*;

public class AppUserService {
  private final AppUserDAO dao = new AppUserDAOImpl();

  public List<AppUser> findAll(Optional<Integer> page, Optional<Integer> size) {
    return dao.findAll(page.orElse(1), size.orElse(10));
  }

  public Optional<AppUser> findById(String id) {
    return dao.findById(id);
  }

  public AppUser create(AppUser user) {
    return dao.create(user);
  }

  public AppUser update(String id, AppUser user) {
    if (dao.findById(id).isEmpty())
      throw new IllegalArgumentException("User not found with ID: " + id);
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
