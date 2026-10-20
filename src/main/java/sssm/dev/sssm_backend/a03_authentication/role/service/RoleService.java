package sssm.dev.sssm_backend.a03_authentication.role.service;

import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a03_authentication.role.entity.Role;
import sssm.dev.sssm_backend.a03_authentication.role.repository.*;

@Service
@RequiredArgsConstructor
public class RoleService {
  private final RoleDAO dao;

  public List<Role> findAll(Optional<Integer> page, Optional<Integer> size) {
    return dao.findAll(page.orElse(1), size.orElse(10));
  }

  public Optional<Role> findById(String id) {
    return dao.findById(id);
  }

  public Role create(Role role) {
    return dao.create(role);
  }

  public Role update(String id, Role role) {
    if (dao.findById(id).isEmpty())
      throw new NotFoundException(
          "Rôle introuvable", "Aucun rôle ne correspond à l'identifiant " + id + ".", "NOT_FOUND");
    role.setId(id);
    return dao.update(role);
  }

  public void delete(String id) {
    dao.delete(id);
  }

  public void assignPermission(String roleId, String permissionId) {
    dao.assignPermission(roleId, permissionId);
  }

  public void removePermission(String roleId, String permissionId) {
    dao.removePermission(roleId, permissionId);
  }
}
