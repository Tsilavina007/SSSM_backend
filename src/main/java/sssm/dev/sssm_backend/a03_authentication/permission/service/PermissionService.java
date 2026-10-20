package sssm.dev.sssm_backend.a03_authentication.permission.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a03_authentication.permission.dto.PermissionCreateRequest;
import sssm.dev.sssm_backend.a03_authentication.permission.dto.PermissionResponse;
import sssm.dev.sssm_backend.a03_authentication.permission.dto.PermissionUpdateRequest;
import sssm.dev.sssm_backend.a03_authentication.permission.entity.Permission;
import sssm.dev.sssm_backend.a03_authentication.permission.repository.PermissionDAO;

@Service
@RequiredArgsConstructor
public class PermissionService {
  private final PermissionDAO dao;

  public List<PermissionResponse> findAll(Optional<Integer> page, Optional<Integer> size) {
    return dao.findAll(page.orElse(1), size.orElse(10)).stream().map(this::toResponse).toList();
  }

  public PermissionResponse findById(String id) {
    return dao.findById(id)
        .map(this::toResponse)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Permission introuvable",
                    "Aucune permission ne correspond à l'identifiant " + id + ".",
                    "NOT_FOUND"));
  }

  public PermissionResponse create(PermissionCreateRequest request) {
    Permission permission = new Permission();
    permission.setCode(request.getCode());
    permission.setName(request.getName());
    permission.setDescription(request.getDescription());
    return toResponse(dao.create(permission));
  }

  public PermissionResponse update(String id, PermissionUpdateRequest request) {
    if (dao.findById(id).isEmpty())
      throw new NotFoundException(
          "Permission introuvable",
          "Aucune permission ne correspond à l'identifiant " + id + ".",
          "NOT_FOUND");
    Permission permission = new Permission();
    permission.setId(id);
    permission.setCode(request.getCode());
    permission.setName(request.getName());
    permission.setDescription(request.getDescription());
    return toResponse(dao.update(permission));
  }

  public void delete(String id) {
    dao.delete(id);
  }

  private PermissionResponse toResponse(Permission permission) {
    return new PermissionResponse(
        permission.getId(), permission.getCode(), permission.getName(), permission.getDescription());
  }
}
