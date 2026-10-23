package sssm.dev.sssm_backend.a06_sites.post_requirement.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import sssm.dev.sssm_backend.a06_sites.post_requirement.dto.PostRequirementCreateRequest;
import sssm.dev.sssm_backend.a06_sites.post_requirement.dto.PostRequirementResponse;
import sssm.dev.sssm_backend.a06_sites.post_requirement.dto.PostRequirementUpdateRequest;
import sssm.dev.sssm_backend.a06_sites.post_requirement.service.PostRequirementService;

@RestController
@RequestMapping("/api/v1/post-requirements")
public class PostRequirementController {
  private final PostRequirementService service;

  PostRequirementController(PostRequirementService service) {
    this.service = service;
  }

  @GetMapping("/post/{postId}")
  public List<PostRequirementResponse> findByPost(@PathVariable String postId) {
    return service.findByPostId(postId);
  }

  @GetMapping("/{id}")
  public PostRequirementResponse findById(@PathVariable String id) {
    return service.findById(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PostRequirementResponse create(@Valid @RequestBody PostRequirementCreateRequest request) {
    return service.create(request);
  }

  @PatchMapping("/{id}")
  public PostRequirementResponse update(
      @PathVariable String id, @Valid @RequestBody PostRequirementUpdateRequest request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable String id) {
    service.delete(id);
  }
}
