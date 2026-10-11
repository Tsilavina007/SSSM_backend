package sssm.dev.sssm_backend.a06_sites.security_post.repository;

import java.util.List;
import java.util.Optional;

import sssm.dev.sssm_backend.a06_sites.security_post.entity.SecurityPost;

public interface SecurityPostDAO {
  List<SecurityPost> findBySiteId(String siteId);

  Optional<SecurityPost> findById(String id);

  SecurityPost create(SecurityPost post);

  SecurityPost update(SecurityPost post);

  void delete(String id);
}
