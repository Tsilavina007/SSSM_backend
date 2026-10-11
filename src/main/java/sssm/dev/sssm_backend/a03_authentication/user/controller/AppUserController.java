package sssm.dev.sssm_backend.a03_authentication.user.controller;

import java.util.*;
import org.springframework.web.bind.annotation.*;
import sssm.dev.sssm_backend.a03_authentication.user.entity.AppUser;
import sssm.dev.sssm_backend.a03_authentication.user.service.AppUserService;

@RestController
@RequestMapping("/api/users")
public class AppUserController {
    private final AppUserService service=new AppUserService();
    @GetMapping public List<AppUser> findAll(@RequestParam Optional<Integer> page,@RequestParam Optional<Integer> pageSize){return service.findAll(page,pageSize);}
    @GetMapping("/{id}") public Optional<AppUser> findById(@PathVariable String id){return service.findById(id);}
    @PostMapping public AppUser create(@RequestBody AppUser user){return service.create(user);}
    @PatchMapping("/{id}") public AppUser update(@PathVariable String id,@RequestBody AppUser user){return service.update(id,user);}
    @DeleteMapping("/{id}") public void delete(@PathVariable String id){service.delete(id);}
    @PostMapping("/{userId}/roles/{roleId}") public void assignRole(@PathVariable String userId,@PathVariable String roleId){service.assignRole(userId,roleId);}
    @DeleteMapping("/{userId}/roles/{roleId}") public void removeRole(@PathVariable String userId,@PathVariable String roleId){service.removeRole(userId,roleId);}
}
