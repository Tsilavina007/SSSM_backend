package sssm.dev.sssm_backend.a06_sites.security_post.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a06_sites.security_post.dto.SecurityPostCreateRequest;
import sssm.dev.sssm_backend.a06_sites.security_post.dto.SecurityPostResponse;
import sssm.dev.sssm_backend.a06_sites.security_post.dto.SecurityPostUpdateRequest;
import sssm.dev.sssm_backend.a06_sites.security_post.entity.SecurityPost;
import sssm.dev.sssm_backend.a06_sites.security_post.repository.SecurityPostDAO;

@Service
@RequiredArgsConstructor
public class SecurityPostService {
  private final SecurityPostDAO dao;

  public List<SecurityPostResponse> findBySiteId(String siteId) {
    return dao.findBySiteId(siteId).stream().map(this::toResponse).toList();
  }

  public SecurityPostResponse findById(String id) {
    return dao.findById(id)
        .map(this::toResponse)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Poste de sécurité introuvable",
                    "Aucun poste de sécurité ne correspond à l'identifiant " + id + ".",
                    "NOT_FOUND"));
  }

  public SecurityPostResponse create(SecurityPostCreateRequest request) {
    return toResponse(dao.create(toEntity(request)));
  }

  public SecurityPostResponse update(String id, SecurityPostUpdateRequest request) {
    if (dao.findById(id).isEmpty())
      throw new NotFoundException(
          "Poste de sécurité introuvable",
          "Aucun poste de sécurité ne correspond à l'identifiant " + id + ".",
          "NOT_FOUND");
    SecurityPost post = toEntity(request);
    post.setId(id);
    return toResponse(dao.update(post));
  }

  public void delete(String id) {
    dao.delete(id);
  }

  private SecurityPost toEntity(SecurityPostCreateRequest request) {
    SecurityPost post = new SecurityPost();
    post.setSite_id(request.getSite_id());
    post.setName(request.getName());
    post.setCode(request.getCode());
    post.setDescription(request.getDescription());
    post.setRequired_agents(request.getRequired_agents());
    post.setStart_time(request.getStart_time());
    post.setEnd_time(request.getEnd_time());
    post.setStatus(request.getStatus());
    return post;
  }

  private SecurityPost toEntity(SecurityPostUpdateRequest request) {
    SecurityPost post = new SecurityPost();
    post.setSite_id(request.getSite_id());
    post.setName(request.getName());
    post.setCode(request.getCode());
    post.setDescription(request.getDescription());
    post.setRequired_agents(request.getRequired_agents());
    post.setStart_time(request.getStart_time());
    post.setEnd_time(request.getEnd_time());
    post.setStatus(request.getStatus());
    return post;
  }

  private SecurityPostResponse toResponse(SecurityPost post) {
    return new SecurityPostResponse(
        post.getId(),
        post.getSite_id(),
        post.getName(),
        post.getCode(),
        post.getDescription(),
        post.getRequired_agents(),
        post.getStart_time(),
        post.getEnd_time(),
        post.getStatus(),
        post.getCreated_at());
  }
}
