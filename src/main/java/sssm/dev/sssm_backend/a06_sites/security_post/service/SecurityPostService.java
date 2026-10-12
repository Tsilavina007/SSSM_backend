package sssm.dev.sssm_backend.a06_sites.security_post.service;

import java.util.List;
import java.util.Optional;

import sssm.dev.sssm_backend.a06_sites.security_post.entity.SecurityPost;
import sssm.dev.sssm_backend.a06_sites.security_post.repository.SecurityPostDAO;
import sssm.dev.sssm_backend.a06_sites.security_post.repository.SecurityPostDAOImpl;

public class SecurityPostService {
  private final SecurityPostDAO dao = new SecurityPostDAOImpl();

  public List<SecurityPost> findBySiteId(String siteId) {
    return dao.findBySiteId(siteId);
  }

  public Optional<SecurityPost> findById(String id) {
    return dao.findById(id);
  }

  public SecurityPost create(SecurityPost post) {
    return dao.create(post);
  }

  public SecurityPost update(String id, SecurityPost post) {
    if (dao.findById(id).isEmpty())
      throw new IllegalArgumentException("Security post not found with ID: " + id);
    post.setId(id);
    return dao.update(post);
  }

  public void delete(String id) {
    dao.delete(id);
  }
}
