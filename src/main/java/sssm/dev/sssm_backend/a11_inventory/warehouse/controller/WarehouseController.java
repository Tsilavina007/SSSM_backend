package sssm.dev.sssm_backend.a11_inventory.warehouse.controller;

import java.util.*;
import org.springframework.web.bind.annotation.*;
import sssm.dev.sssm_backend.a11_inventory.warehouse.entity.Warehouse;
import sssm.dev.sssm_backend.a11_inventory.warehouse.service.WarehouseService;

@RestController
@RequestMapping("/api/warehouses")
public class WarehouseController {
  private final WarehouseService service = new WarehouseService();

  @GetMapping
  public List<Warehouse> findAll(
      @RequestParam Optional<String> search,
      @RequestParam Optional<Integer> page,
      @RequestParam Optional<Integer> pageSize) {
    return service.findAll(search, page, pageSize);
  }

  @GetMapping("/{id}")
  public Optional<Warehouse> findById(@PathVariable String id) {
    return service.findById(id);
  }

  @PostMapping
  public Warehouse create(@RequestBody Warehouse v) {
    return service.create(v);
  }

  @PatchMapping("/{id}")
  public Warehouse update(@PathVariable String id, @RequestBody Warehouse v) {
    return service.update(id, v);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable String id) {
    service.delete(id);
  }
}
