package sssm.dev.sssm_backend.a06_sites.post_requirement.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a06_sites.post_requirement.entity.PostRequirement;
import sssm.dev.sssm_backend.a06_sites.post_requirement.repository.PostRequirementDAO;

@Service
@RequiredArgsConstructor
public class PostRequirementService {
  private final PostRequirementDAO dao;

  public List<PostRequirement> findByPostId(String postId) {
    return dao.findByPostId(postId);
  }

  public Optional<PostRequirement> findById(String id) {
    return dao.findById(id);
  }

  public PostRequirement create(PostRequirement requirement) {
    return dao.create(requirement);
  }

  public PostRequirement update(String id, PostRequirement requirement) {
    if (dao.findById(id).isEmpty())
      throw new NotFoundException(
          "Exigence de poste introuvable",
          "Aucune exigence de poste ne correspond à l'identifiant " + id + ".",
          "NOT_FOUND");
    requirement.setId(id);
    return dao.update(requirement);
  }

  public void delete(String id) {
    dao.delete(id);
  }
}
