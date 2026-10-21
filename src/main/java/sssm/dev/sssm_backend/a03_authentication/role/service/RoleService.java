package sssm.dev.sssm_backend.a03_authentication.role.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ClientException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a03_authentication.auth.security.CurrentUser;
import sssm.dev.sssm_backend.a03_authentication.role.dto.RoleCreateRequest;
import sssm.dev.sssm_backend.a03_authentication.role.dto.RoleResponse;
import sssm.dev.sssm_backend.a03_authentication.role.dto.RoleUpdateRequest;
import sssm.dev.sssm_backend.a03_authentication.role.entity.Role;
import sssm.dev.sssm_backend.a03_authentication.role.repository.RoleDAO;

@Service
@RequiredArgsConstructor
public class RoleService {
  private final RoleDAO dao;
  private final CurrentUser currentUser;

  public List<RoleResponse> findAll(Optional<Integer> page, Optional<Integer> size) {
    int requestedPage = page.orElse(1);
    int requestedSize = size.orElse(10);
    List<Role> roles =
        currentUser.isPlatformAdmin()
            ? dao.findAll(requestedPage, requestedSize)
            : dao.findAllByCompany(currentUser.requireCompanyId(), requestedPage, requestedSize);
    return roles.stream().map(this::toResponse).toList();
  }

  public RoleResponse findById(String id) {
    return toResponse(requireAccessibleRole(id));
  }

  public RoleResponse create(RoleCreateRequest request) {
    Role role = toEntity(request);
    role.setCompany_id(resolveCompanyForWrite(request.getCompany_id()));
    return toResponse(dao.create(role));
  }

  public RoleResponse update(String id, RoleUpdateRequest request) {
    requireAccessibleRole(id);
    Role role = toEntity(request);
    role.setId(id);
    role.setCompany_id(resolveCompanyForWrite(request.getCompany_id()));
    return toResponse(dao.update(role));
  }

  public void delete(String id) {
    requireAccessibleRole(id);
    dao.delete(id);
  }

  public void assignPermission(String roleId, String permissionId) {
    requireAccessibleRole(roleId);
    dao.assignPermission(roleId, permissionId);
  }

  public void removePermission(String roleId, String permissionId) {
    requireAccessibleRole(roleId);
    dao.removePermission(roleId, permissionId);
  }

  private String resolveCompanyForWrite(String requestedCompanyId) {
    if (currentUser.isPlatformAdmin()) {
      return requestedCompanyId;
    }
    String companyId = currentUser.requireCompanyId();
    if (requestedCompanyId != null && !requestedCompanyId.equals(companyId)) {
      throw new ClientException(
          "Entreprise non autorisée",
          "Un administrateur d'entreprise ne peut gérer que les rôles de sa propre entreprise.",
          "CLIENT_ERROR");
    }
    return companyId;
  }

  private Role requireAccessibleRole(String id) {
    Role role =
        dao.findById(id)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Rôle introuvable",
                        "Aucun rôle ne correspond à l'identifiant " + id + ".",
                        "NOT_FOUND"));
    if (currentUser.isPlatformAdmin()) return role;

    String companyId = currentUser.requireCompanyId();
    if (!companyId.equals(role.getCompany_id())) {
      throw new NotFoundException(
          "Rôle introuvable", "Aucun rôle ne correspond à l'identifiant " + id + ".", "NOT_FOUND");
    }
    return role;
  }

  private Role toEntity(RoleCreateRequest request) {
    Role role = new Role();
    role.setName(request.getName());
    role.setDescription(request.getDescription());
    return role;
  }

  private Role toEntity(RoleUpdateRequest request) {
    Role role = new Role();
    role.setName(request.getName());
    role.setDescription(request.getDescription());
    return role;
  }

  private RoleResponse toResponse(Role role) {
    return new RoleResponse(
        role.getId(), role.getCompany_id(), role.getName(), role.getDescription());
  }
}
