package sssm.dev.sssm_backend.a03_authentication.role.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a03_authentication.role.dto.RoleCreateRequest;
import sssm.dev.sssm_backend.a03_authentication.role.dto.RoleResponse;
import sssm.dev.sssm_backend.a03_authentication.role.dto.RoleUpdateRequest;
import sssm.dev.sssm_backend.a03_authentication.role.entity.Role;
import sssm.dev.sssm_backend.a03_authentication.role.repository.RoleDAO;

@Service
@RequiredArgsConstructor
public class RoleService {
  private final RoleDAO dao;

  public List<RoleResponse> findAll(Optional<Integer> page, Optional<Integer> size) {
    return dao.findAll(page.orElse(1), size.orElse(10)).stream().map(this::toResponse).toList();
  }

  public RoleResponse findById(String id) {
    return dao.findById(id)
        .map(this::toResponse)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Rôle introuvable",
                    "Aucun rôle ne correspond à l'identifiant " + id + ".",
                    "NOT_FOUND"));
  }

  public RoleResponse create(RoleCreateRequest request) {
    return toResponse(dao.create(toEntity(request)));
  }

  public RoleResponse update(String id, RoleUpdateRequest request) {
    if (dao.findById(id).isEmpty())
      throw new NotFoundException(
          "Rôle introuvable", "Aucun rôle ne correspond à l'identifiant " + id + ".", "NOT_FOUND");
    Role role = toEntity(request);
    role.setId(id);
    return toResponse(dao.update(role));
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

  private Role toEntity(RoleCreateRequest request) {
    Role role = new Role();
    role.setCompany_id(request.getCompany_id());
    role.setName(request.getName());
    role.setDescription(request.getDescription());
    return role;
  }

  private Role toEntity(RoleUpdateRequest request) {
    Role role = new Role();
    role.setCompany_id(request.getCompany_id());
    role.setName(request.getName());
    role.setDescription(request.getDescription());
    return role;
  }

  private RoleResponse toResponse(Role role) {
    return new RoleResponse(
        role.getId(), role.getCompany_id(), role.getName(), role.getDescription());
  }
}
