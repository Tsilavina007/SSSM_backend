package sssm.dev.sssm_backend.a06_sites.site.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a06_sites.site.entity.Site;
import sssm.dev.sssm_backend.a06_sites.site.repository.SiteDAO;

@Service
@RequiredArgsConstructor
public class SiteService {
  private final SiteDAO dao;

  public List<Site> findAll(
      Optional<String> search, Optional<Integer> page, Optional<Integer> size) {
    int currentPage = page.orElse(1);
    int pageSize = size.orElse(10);
    return search
        .map(value -> dao.search(value, currentPage, pageSize))
        .orElseGet(() -> dao.findAll(currentPage, pageSize));
  }

  public Optional<Site> findById(String id) {
    return dao.findById(id);
  }

  public Site create(Site site) {
    return dao.create(site);
  }

  public Site update(String id, Site site) {
    if (dao.findById(id).isEmpty()) {
      throw new NotFoundException(
          "Site introuvable", "Aucun site ne correspond à l'identifiant " + id + ".", "NOT_FOUND");
    }
    site.setId(id);
    return dao.update(site);
  }

  public void delete(String id) {
    dao.delete(id);
  }
}
