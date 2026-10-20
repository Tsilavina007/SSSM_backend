package sssm.dev.sssm_backend.a06_sites.security_post.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import sssm.dev.sssm_backend.a06_sites.security_post.dto.SecurityPostCreateRequest;
import sssm.dev.sssm_backend.a06_sites.security_post.dto.SecurityPostResponse;
import sssm.dev.sssm_backend.a06_sites.security_post.dto.SecurityPostUpdateRequest;
import sssm.dev.sssm_backend.a06_sites.security_post.service.SecurityPostService;

@RestController
@RequestMapping("/api/security-posts")
public class SecurityPostController {
  private final SecurityPostService service;

  SecurityPostController(SecurityPostService service) {
    this.service = service;
  }

  @GetMapping("/site/{siteId}")
  public List<SecurityPostResponse> findBySite(@PathVariable String siteId) {
    return service.findBySiteId(siteId);
  }

  @GetMapping("/{id}")
  public SecurityPostResponse findById(@PathVariable String id) {
    return service.findById(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public SecurityPostResponse create(@Valid @RequestBody SecurityPostCreateRequest request) {
    return service.create(request);
  }

  @PatchMapping("/{id}")
  public SecurityPostResponse update(
      @PathVariable String id, @Valid @RequestBody SecurityPostUpdateRequest request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable String id) {
    service.delete(id);
  }
}
