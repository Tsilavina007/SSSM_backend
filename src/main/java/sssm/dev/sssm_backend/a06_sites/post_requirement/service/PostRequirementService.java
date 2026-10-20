package sssm.dev.sssm_backend.a06_sites.post_requirement.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a06_sites.post_requirement.dto.PostRequirementCreateRequest;
import sssm.dev.sssm_backend.a06_sites.post_requirement.dto.PostRequirementResponse;
import sssm.dev.sssm_backend.a06_sites.post_requirement.dto.PostRequirementUpdateRequest;
import sssm.dev.sssm_backend.a06_sites.post_requirement.entity.PostRequirement;
import sssm.dev.sssm_backend.a06_sites.post_requirement.repository.PostRequirementDAO;

@Service
@RequiredArgsConstructor
public class PostRequirementService {
  private final PostRequirementDAO dao;

  public List<PostRequirementResponse> findByPostId(String postId) {
    return dao.findByPostId(postId).stream().map(this::toResponse).toList();
  }

  public PostRequirementResponse findById(String id) {
    return dao.findById(id)
        .map(this::toResponse)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Exigence de poste introuvable",
                    "Aucune exigence de poste ne correspond à l'identifiant " + id + ".",
                    "NOT_FOUND"));
  }

  public PostRequirementResponse create(PostRequirementCreateRequest request) {
    PostRequirement requirement = new PostRequirement();
    requirement.setPost_id(request.getPost_id());
    requirement.setRequirement(request.getRequirement());
    return toResponse(dao.create(requirement));
  }

  public PostRequirementResponse update(String id, PostRequirementUpdateRequest request) {
    if (dao.findById(id).isEmpty())
      throw new NotFoundException(
          "Exigence de poste introuvable",
          "Aucune exigence de poste ne correspond à l'identifiant " + id + ".",
          "NOT_FOUND");
    PostRequirement requirement = new PostRequirement();
    requirement.setId(id);
    requirement.setRequirement(request.getRequirement());
    return toResponse(dao.update(requirement));
  }

  public void delete(String id) {
    dao.delete(id);
  }

  private PostRequirementResponse toResponse(PostRequirement requirement) {
    return new PostRequirementResponse(
        requirement.getId(), requirement.getPost_id(), requirement.getRequirement());
  }
}
