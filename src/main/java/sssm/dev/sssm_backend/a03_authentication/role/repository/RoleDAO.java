package sssm.dev.sssm_backend.a03_authentication.role.repository;

import java.util.List;
import java.util.Optional;

import sssm.dev.sssm_backend.a03_authentication.role.entity.Role;

public interface RoleDAO {
  List<Role> findAll(int page, int pageSize);

  Optional<Role> findById(String id);

  Optional<Role> findGlobalByName(String name);

  Role create(Role role);

  Role update(Role role);

  void delete(String id);

  void assignPermission(String roleId, String permissionId);

  void removePermission(String roleId, String permissionId);
}
