package sssm.dev.sssm_backend.a06_sites.security_post.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a06_sites.security_post.entity.SecurityPost;
import sssm.dev.sssm_backend.a06_sites.security_post.repository.SecurityPostDAO;

@Service
@RequiredArgsConstructor
public class SecurityPostService {
  private final SecurityPostDAO dao;

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
      throw new NotFoundException(
          "Poste de sécurité introuvable",
          "Aucun poste de sécurité ne correspond à l'identifiant " + id + ".",
          "NOT_FOUND");
    post.setId(id);
    return dao.update(post);
  }

  public void delete(String id) {
    dao.delete(id);
  }
}
