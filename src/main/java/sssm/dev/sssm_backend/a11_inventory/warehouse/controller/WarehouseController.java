package sssm.dev.sssm_backend.a11_inventory.warehouse.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import sssm.dev.sssm_backend.a11_inventory.warehouse.dto.WarehouseCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.warehouse.dto.WarehouseUpdateRequest;
import sssm.dev.sssm_backend.a11_inventory.warehouse.dto.rest.WarehouseRest;
import sssm.dev.sssm_backend.a11_inventory.warehouse.service.WarehouseService;

@RestController
@RequestMapping("/api/inventory/warehouses")
public class WarehouseController {
  private final WarehouseService warehouseService;

  WarehouseController(WarehouseService warehouseService) {
    this.warehouseService = warehouseService;
  }

  @GetMapping
  public List<WarehouseRest> getWarehouses(
      @RequestParam Optional<Integer> page,
      @RequestParam Optional<Integer> pageSize,
      @RequestParam Optional<String> search) {
    return warehouseService.getWarehouses(search, page, pageSize);
  }

  @GetMapping("/{warehouseId}")
  public WarehouseRest getWarehouseById(@PathVariable String warehouseId) {
    return warehouseService.getWarehouseById(warehouseId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public WarehouseRest createWarehouse(@Valid @RequestBody WarehouseCreateRequest warehouse) {
    return warehouseService.createWarehouse(warehouse);
  }

  @PatchMapping("/{warehouseId}")
  public WarehouseRest updateWarehouse(
      @PathVariable String warehouseId,
      @Valid @RequestBody WarehouseUpdateRequest updatedWarehouse) {
    return warehouseService.updateWarehouse(warehouseId, updatedWarehouse);
  }

  @PostMapping("/{warehouseId}/restore")
  public WarehouseRest restoreWarehouse(@PathVariable String warehouseId) {
    return warehouseService.restoreWarehouse(warehouseId);
  }

  @DeleteMapping("/{warehouseId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteWarehouse(@PathVariable String warehouseId) {
    warehouseService.deleteWarehouse(warehouseId);
  }
}
