package sssm.dev.sssm_backend.a11_inventory.item_category.controller;

import java.util.*;
import org.springframework.web.bind.annotation.*;
import sssm.dev.sssm_backend.a11_inventory.item_category.entity.ItemCategory;
import sssm.dev.sssm_backend.a11_inventory.item_category.service.ItemCategoryService;

@RestController
@RequestMapping("/api/item-categories")
public class ItemCategoryController {
  private final ItemCategoryService service = new ItemCategoryService();

  @GetMapping
  public List<ItemCategory> findAll(
      @RequestParam Optional<String> search,
      @RequestParam Optional<Integer> page,
      @RequestParam Optional<Integer> pageSize) {
    return service.findAll(search, page, pageSize);
  }

  @GetMapping("/{id}")
  public Optional<ItemCategory> findById(@PathVariable String id) {
    return service.findById(id);
  }

  @PostMapping
  public ItemCategory create(@RequestBody ItemCategory v) {
    return service.create(v);
  }

  @PatchMapping("/{id}")
  public ItemCategory update(@PathVariable String id, @RequestBody ItemCategory v) {
    return service.update(id, v);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable String id) {
    service.delete(id);
  }
}
