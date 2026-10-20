package sssm.dev.sssm_backend.a03_authentication.permission.service;

import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a03_authentication.permission.entity.Permission;
import sssm.dev.sssm_backend.a03_authentication.permission.repository.*;

@Service
@RequiredArgsConstructor
public class PermissionService {
  private final PermissionDAO dao;

  public List<Permission> findAll(Optional<Integer> page, Optional<Integer> size) {
    return dao.findAll(page.orElse(1), size.orElse(10));
  }

  public Optional<Permission> findById(String id) {
    return dao.findById(id);
  }

  public Permission create(Permission permission) {
    return dao.create(permission);
  }

  public Permission update(String id, Permission permission) {
    if (dao.findById(id).isEmpty())
      throw new NotFoundException(
          "Permission introuvable",
          "Aucune permission ne correspond à l'identifiant " + id + ".",
          "NOT_FOUND");
    permission.setId(id);
    return dao.update(permission);
  }

  public void delete(String id) {
    dao.delete(id);
  }
}
