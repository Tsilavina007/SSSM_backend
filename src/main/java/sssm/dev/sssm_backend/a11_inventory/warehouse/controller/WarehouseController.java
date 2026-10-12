package sssm.dev.sssm_backend.a11_inventory.warehouse.controller;

import java.util.List;
import java.util.Optional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sssm.dev.sssm_backend.a11_inventory.warehouse.entity.Warehouse;
import sssm.dev.sssm_backend.a11_inventory.warehouse.service.WarehouseService;

@RestController
@RequestMapping({"/api/warehouses", "/api/inventory/warehouses"})
public class WarehouseController {
  private final WarehouseService warehouseService;

  public WarehouseController() {
    this.warehouseService = new WarehouseService();
  }

  @GetMapping
  public List<Warehouse> getWarehouses(
      @RequestParam Optional<Integer> page,
      @RequestParam Optional<Integer> pageSize,
      @RequestParam Optional<String> search) {
    return warehouseService.getWarehouses(search, page, pageSize);
  }

  @GetMapping("/{warehouseId}")
  public Optional<Warehouse> getWarehouseById(@PathVariable String warehouseId) {
    return warehouseService.getWarehouseById(warehouseId);
  }

  @PostMapping
  public Warehouse createWarehouse(@RequestBody Warehouse warehouse) {
    return warehouseService.createWarehouse(warehouse);
  }

  @PatchMapping("/{warehouseId}")
  public Warehouse updateWarehouse(
      @PathVariable String warehouseId, @RequestBody Warehouse updatedWarehouse) {
    return warehouseService.updateWarehouse(warehouseId, updatedWarehouse);
  }

  @DeleteMapping("/{warehouseId}")
  public void deleteWarehouse(@PathVariable String warehouseId) {
    warehouseService.deleteWarehouse(warehouseId);
  }
}
