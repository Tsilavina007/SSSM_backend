package sssm.dev.sssm_backend.a06_sites.post_requirement.repository;

import java.util.List;
import java.util.Optional;
import sssm.dev.sssm_backend.a06_sites.post_requirement.entity.PostRequirement;

public interface PostRequirementDAO {
  List<PostRequirement> findByPostId(String postId);

  Optional<PostRequirement> findById(String id);

  PostRequirement create(PostRequirement requirement);

  PostRequirement update(PostRequirement requirement);

  void delete(String id);
}
