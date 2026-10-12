package sssm.dev.sssm_backend.a06_sites.site.repository;

import java.util.List;
import java.util.Optional;

import sssm.dev.sssm_backend.a06_sites.site.entity.Site;

public interface SiteDAO {
  List<Site> findAll(int page, int pageSize);

  List<Site> search(String value, int page, int pageSize);

  Optional<Site> findById(String id);

  Site create(Site site);

  Site update(Site site);

  void delete(String id);
}
