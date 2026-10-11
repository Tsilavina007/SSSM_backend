package sssm.dev.sssm_backend.a06_sites.site.service;

import java.util.List;
import java.util.Optional;
import sssm.dev.sssm_backend.a06_sites.site.entity.Site;
import sssm.dev.sssm_backend.a06_sites.site.repository.SiteDAO;
import sssm.dev.sssm_backend.a06_sites.site.repository.SiteDAOImpl;

public class SiteService {
  private final SiteDAO dao = new SiteDAOImpl();

  public List<Site> findAll(
      Optional<String> search, Optional<Integer> page, Optional<Integer> size) {
    int currentPage = page.orElse(1);
    int pageSize = size.orElse(10);
    return search.isPresent()
        ? dao.search(search.get(), currentPage, pageSize)
        : dao.findAll(currentPage, pageSize);
  }

  public Optional<Site> findById(String id) {
    return dao.findById(id);
  }

  public Site create(Site site) {
    return dao.create(site);
  }

  public Site update(String id, Site site) {
    if (dao.findById(id).isEmpty()) {
      throw new IllegalArgumentException("Site not found with ID: " + id);
    }
    site.setId(id);
    return dao.update(site);
  }

  public void delete(String id) {
    dao.delete(id);
  }
}
