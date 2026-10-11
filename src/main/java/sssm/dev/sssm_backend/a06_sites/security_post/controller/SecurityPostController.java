package sssm.dev.sssm_backend.a06_sites.security_post.controller;

import java.util.List;
import java.util.Optional;
import org.springframework.web.bind.annotation.*;

import sssm.dev.sssm_backend.a06_sites.security_post.entity.SecurityPost;
import sssm.dev.sssm_backend.a06_sites.security_post.service.SecurityPostService;

@RestController
@RequestMapping("/api/security-posts")
public class SecurityPostController {
    private final SecurityPostService service = new SecurityPostService();
    @GetMapping("/site/{siteId}") public List<SecurityPost> findBySite(@PathVariable String siteId) { return service.findBySiteId(siteId); }
    @GetMapping("/{id}") public Optional<SecurityPost> findById(@PathVariable String id) { return service.findById(id); }
    @PostMapping public SecurityPost create(@RequestBody SecurityPost post) { return service.create(post); }
    @PatchMapping("/{id}") public SecurityPost update(@PathVariable String id, @RequestBody SecurityPost post) { return service.update(id, post); }
    @DeleteMapping("/{id}") public void delete(@PathVariable String id) { service.delete(id); }
}
