package sssm.dev.sssm_backend.a06_sites.site.controller;

import java.util.List;
import java.util.Optional;
import org.springframework.web.bind.annotation.*;
import sssm.dev.sssm_backend.a06_sites.site.entity.Site;
import sssm.dev.sssm_backend.a06_sites.site.service.SiteService;

@RestController
@RequestMapping("/api/sites")
public class SiteController {
  private final SiteService service = new SiteService();

  @GetMapping
  public List<Site> findAll(
      @RequestParam Optional<String> search,
      @RequestParam Optional<Integer> page,
      @RequestParam Optional<Integer> pageSize) {
    return service.findAll(search, page, pageSize);
  }

  @GetMapping("/{id}")
  public Optional<Site> findById(@PathVariable String id) {
    return service.findById(id);
  }

  @PostMapping
  public Site create(@RequestBody Site site) {
    return service.create(site);
  }

  @PatchMapping("/{id}")
  public Site update(@PathVariable String id, @RequestBody Site site) {
    return service.update(id, site);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable String id) {
    service.delete(id);
  }
}
