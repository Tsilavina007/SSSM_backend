package sssm.dev.sssm_backend.a11_inventory.category.controller;

import java.util.*;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sssm.dev.sssm_backend.a11_inventory.category.service.CategoryService;

@RestController
@RequestMapping("/api/inventory")
public class CategoryController {
  private final CategoryService service = new CategoryService();

  @GetMapping("/categories")
  public Object list(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer pageSize) {
    return service.list(page, pageSize, search, null);
  }

  @PostMapping("/categories")
  public Object create(@RequestBody Map<String, Object> b) {
    return service.save(null, b, false);
  }

  @GetMapping("/categories/{id}")
  public Object get(@PathVariable String id) {
    return service.one(id);
  }

  @PutMapping("/categories/{id}")
  public Object put(@PathVariable String id, @RequestBody Map<String, Object> b) {
    return service.save(id, b, true);
  }

  @DeleteMapping("/categories/{id}")
  public Object del(@PathVariable String id) {
    return service.delete(id);
  }

  @GetMapping("/categories/{id}/types")
  public Object types(
      @PathVariable String id,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer pageSize) {
    return service.list(page, pageSize, null, "category_id=?", id);
  }
}
