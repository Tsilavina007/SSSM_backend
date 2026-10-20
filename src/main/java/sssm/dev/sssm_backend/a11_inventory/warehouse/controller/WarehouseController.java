package sssm.dev.sssm_backend.a11_inventory.warehouse.controller;

import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sssm.dev.sssm_backend.a11_inventory.warehouse.dto.WarehouseCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.warehouse.dto.WarehouseUpdateRequest;
import sssm.dev.sssm_backend.a11_inventory.warehouse.entity.Warehouse;
import sssm.dev.sssm_backend.a11_inventory.warehouse.service.WarehouseService;

@RestController
@RequestMapping("/api/inventory/warehouses")
public class WarehouseController {
  private final WarehouseService warehouseService;

  WarehouseController(WarehouseService warehouseService) {
    this.warehouseService = warehouseService;
  }

  @GetMapping
  public ResponseEntity<Object> getWarehouses(
      @RequestParam Optional<Integer> page,
      @RequestParam Optional<Integer> pageSize,
      @RequestParam Optional<String> search) {
    List<Warehouse> warehouses = warehouseService.getWarehouses(search, page, pageSize);
    return ResponseEntity.ok().body(warehouses);
  }

  @GetMapping("/{warehouseId}")
  public ResponseEntity<Object> getWarehouseById(@PathVariable String warehouseId) {
    Optional<Warehouse> warehouses = warehouseService.getWarehouseById(warehouseId);
    return ResponseEntity.ok().body(warehouses);
  }

  @PostMapping
  public ResponseEntity<Object> createWarehouse(@RequestBody WarehouseCreateRequest warehouse) {
    Warehouse warehouses = warehouseService.createWarehouse(warehouse);
    return ResponseEntity.ok().body(warehouses);
  }

  @PatchMapping("/{warehouseId}")
  public ResponseEntity<Object> updateWarehouse(
      @PathVariable String warehouseId, @RequestBody WarehouseUpdateRequest updatedWarehouse) {
    Warehouse warehouses = warehouseService.updateWarehouse(warehouseId, updatedWarehouse);
    return ResponseEntity.ok().body(warehouses);
  }

  @DeleteMapping("/{warehouseId}")
  public ResponseEntity<Object> deleteWarehouse(@PathVariable String warehouseId) {
    warehouseService.deleteWarehouse(warehouseId);
    return ResponseEntity.ok().body("Warehouse deleted : " + warehouseId);
  }
}
