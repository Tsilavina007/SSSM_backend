package sssm.dev.sssm_backend.a11_inventory.inventory_count.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sssm.dev.sssm_backend.a11_inventory.inventory_count.dto.InventoryCountCloseRequest;
import sssm.dev.sssm_backend.a11_inventory.inventory_count.dto.InventoryCountCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.inventory_count.dto.InventoryCountScanRequest;
import sssm.dev.sssm_backend.a11_inventory.inventory_count.dto.rest.InventoryCountRest;
import sssm.dev.sssm_backend.a11_inventory.inventory_count.dto.rest.InventoryCountScanRest;
import sssm.dev.sssm_backend.a11_inventory.inventory_count.service.InventoryCountService;

@RestController
@RequestMapping("/api/inventory/inventory-counts")
public class InventoryCountController {
  private final InventoryCountService inventoryCountService;

  InventoryCountController(InventoryCountService inventoryCountService) {
    this.inventoryCountService = inventoryCountService;
  }

  @GetMapping
  public List<InventoryCountRest> list(
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer pageSize) {
    return inventoryCountService.list(page, pageSize, null, null);
  }

  @PostMapping
  public ResponseEntity<Object> create(@RequestBody InventoryCountCreateRequest request) {
    return ResponseEntity.ok().body(inventoryCountService.save(null, request.toMap(), false));
  }

  @PostMapping("/{id}/scans")
  public InventoryCountScanRest scan(
      @PathVariable String id, @RequestBody InventoryCountScanRequest request) {
    return inventoryCountService.scan(id, request.toMap());
  }

  @GetMapping("/{id}/results")
  public List<InventoryCountScanRest> results(@PathVariable String id) {
    return inventoryCountService.getResults(id);
  }

  @PostMapping("/{id}/close")
  public ResponseEntity<Object> close(
      @PathVariable String id, @RequestBody(required = false) InventoryCountCloseRequest request) {
    return ResponseEntity.ok()
        .body(inventoryCountService.close(id, request == null ? null : request.toMap()));
  }
}
