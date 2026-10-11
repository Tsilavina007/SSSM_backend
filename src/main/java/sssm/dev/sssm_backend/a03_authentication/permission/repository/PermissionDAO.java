package sssm.dev.sssm_backend.a03_authentication.permission.repository;

import java.util.List;
import java.util.Optional;

import sssm.dev.sssm_backend.a03_authentication.permission.entity.Permission;

public interface PermissionDAO {
    List<Permission> findAll(int page, int pageSize);
    Optional<Permission> findById(String id);
    Permission create(Permission permission);
    Permission update(Permission permission);
    void delete(String id);
}
