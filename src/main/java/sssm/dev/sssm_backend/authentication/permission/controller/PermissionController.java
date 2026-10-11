package sssm.dev.sssm_backend.authentication.permission.controller;

import java.util.*;
import org.springframework.web.bind.annotation.*;
import sssm.dev.sssm_backend.authentication.permission.entity.Permission;
import sssm.dev.sssm_backend.authentication.permission.service.PermissionService;

@RestController
@RequestMapping("/api/permissions")
public class PermissionController {
    private final PermissionService service=new PermissionService();
    @GetMapping public List<Permission> findAll(@RequestParam Optional<Integer> page,@RequestParam Optional<Integer> pageSize){return service.findAll(page,pageSize);}
    @GetMapping("/{id}") public Optional<Permission> findById(@PathVariable String id){return service.findById(id);}
    @PostMapping public Permission create(@RequestBody Permission permission){return service.create(permission);}
    @PatchMapping("/{id}") public Permission update(@PathVariable String id,@RequestBody Permission permission){return service.update(id,permission);}
    @DeleteMapping("/{id}") public void delete(@PathVariable String id){service.delete(id);}
}
