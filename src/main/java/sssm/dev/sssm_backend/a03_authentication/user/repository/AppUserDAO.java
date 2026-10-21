package sssm.dev.sssm_backend.a03_authentication.user.repository;

import java.util.List;
import java.util.Optional;
import sssm.dev.sssm_backend.a03_authentication.user.entity.AppUser;

public interface AppUserDAO {
  List<AppUser> findAll(int page, int pageSize);

  List<AppUser> findAllByCompany(String companyId, int page, int pageSize);

  Optional<AppUser> findById(String id);

  Optional<AppUser> findByUsername(String username);

  Optional<AppUser> findByUsernameOrEmail(String login);

  List<String> findRoleNames(String userId);

  List<String> findPermissionCodes(String userId);

  boolean isPlatformAdmin(String userId);

  void updateLastLogin(String userId);

  AppUser create(AppUser user);

  AppUser update(AppUser user);

  void delete(String id);

  void assignRole(String userId, String roleId);

  void removeRole(String userId, String roleId);
}
