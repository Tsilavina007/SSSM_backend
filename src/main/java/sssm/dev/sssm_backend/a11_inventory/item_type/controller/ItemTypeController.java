package sssm.dev.sssm_backend.a11_inventory.item_type.controller;

import java.util.*;
import org.springframework.web.bind.annotation.*;
import sssm.dev.sssm_backend.a11_inventory.item_type.entity.ItemType;
import sssm.dev.sssm_backend.a11_inventory.item_type.service.ItemTypeService;

@RestController
@RequestMapping("/api/item-types")
public class ItemTypeController {
  private final ItemTypeService service = new ItemTypeService();

  @GetMapping
  public List<ItemType> findAll(
      @RequestParam Optional<String> search,
      @RequestParam Optional<Integer> page,
      @RequestParam Optional<Integer> pageSize) {
    return service.findAll(search, page, pageSize);
  }

  @GetMapping("/{id}")
  public Optional<ItemType> findById(@PathVariable String id) {
    return service.findById(id);
  }

  @PostMapping
  public ItemType create(@RequestBody ItemType v) {
    return service.create(v);
  }

  @PatchMapping("/{id}")
  public ItemType update(@PathVariable String id, @RequestBody ItemType v) {
    return service.update(id, v);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable String id) {
    service.delete(id);
  }
}
