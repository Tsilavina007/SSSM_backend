package sssm.dev.sssm_backend.a06_sites.site.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import sssm.dev.sssm_backend.a06_sites.site.dto.SiteCreateRequest;
import sssm.dev.sssm_backend.a06_sites.site.dto.SiteResponse;
import sssm.dev.sssm_backend.a06_sites.site.dto.SiteUpdateRequest;
import sssm.dev.sssm_backend.a06_sites.site.service.SiteService;

@RestController
@RequestMapping("/api/v1/sites")
public class SiteController {
  private final SiteService service;

  SiteController(SiteService service) {
    this.service = service;
  }

  @GetMapping
  public List<SiteResponse> findAll(
      @RequestParam Optional<String> search,
      @RequestParam Optional<Integer> page,
      @RequestParam Optional<Integer> pageSize) {
    return service.findAll(search, page, pageSize);
  }

  @GetMapping("/{id}")
  public SiteResponse findById(@PathVariable String id) {
    return service.findById(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public SiteResponse create(@Valid @RequestBody SiteCreateRequest request) {
    return service.create(request);
  }

  @PatchMapping("/{id}")
  public SiteResponse update(
      @PathVariable String id, @Valid @RequestBody SiteUpdateRequest request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable String id) {
    service.delete(id);
  }
}
