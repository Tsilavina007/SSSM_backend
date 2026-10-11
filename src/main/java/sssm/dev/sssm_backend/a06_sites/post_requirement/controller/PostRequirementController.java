package sssm.dev.sssm_backend.a06_sites.post_requirement.controller;

import java.util.List;
import java.util.Optional;
import org.springframework.web.bind.annotation.*;
import sssm.dev.sssm_backend.a06_sites.post_requirement.entity.PostRequirement;
import sssm.dev.sssm_backend.a06_sites.post_requirement.service.PostRequirementService;

@RestController
@RequestMapping("/api/post-requirements")
public class PostRequirementController {
  private final PostRequirementService service = new PostRequirementService();

  @GetMapping("/post/{postId}")
  public List<PostRequirement> findByPost(@PathVariable String postId) {
    return service.findByPostId(postId);
  }

  @GetMapping("/{id}")
  public Optional<PostRequirement> findById(@PathVariable String id) {
    return service.findById(id);
  }

  @PostMapping
  public PostRequirement create(@RequestBody PostRequirement requirement) {
    return service.create(requirement);
  }

  @PatchMapping("/{id}")
  public PostRequirement update(@PathVariable String id, @RequestBody PostRequirement requirement) {
    return service.update(id, requirement);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable String id) {
    service.delete(id);
  }
}
