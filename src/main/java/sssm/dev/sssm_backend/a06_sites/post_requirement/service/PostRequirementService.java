package sssm.dev.sssm_backend.a06_sites.post_requirement.service;

import java.util.List;
import java.util.Optional;
import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a06_sites.post_requirement.entity.PostRequirement;
import sssm.dev.sssm_backend.a06_sites.post_requirement.repository.PostRequirementDAO;
import sssm.dev.sssm_backend.a06_sites.post_requirement.repository.PostRequirementDAOImpl;

public class PostRequirementService {
  private final PostRequirementDAO dao = new PostRequirementDAOImpl();

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
      throw new NotFoundException("Post requirement not found with ID: " + id);
    requirement.setId(id);
    return dao.update(requirement);
  }

  public void delete(String id) {
    dao.delete(id);
  }
}
