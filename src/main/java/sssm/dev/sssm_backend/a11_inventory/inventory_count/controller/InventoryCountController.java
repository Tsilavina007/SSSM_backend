package sssm.dev.sssm_backend.a11_inventory.inventory_count.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sssm.dev.sssm_backend.a11_inventory.inventory_count.service.InventoryCountService;

@RestController
@RequestMapping("/api/inventory/inventory-counts")
public class InventoryCountController {
  private final InventoryCountService inventoryCountService;

  InventoryCountController(InventoryCountService inventoryCountService) {
    this.inventoryCountService = inventoryCountService;
  }

  @GetMapping
  public ResponseEntity<Object> list(
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer pageSize) {
    return ResponseEntity.ok().body(inventoryCountService.list(page, pageSize, null, null));
  }

  @PostMapping
  public ResponseEntity<Object> create(@RequestBody Map<String, Object> b) {
    return ResponseEntity.ok().body(inventoryCountService.save(null, b, false));
  }

  @PostMapping("/{id}/scans")
  public ResponseEntity<Object> scan(@PathVariable String id, @RequestBody Map<String, Object> b) {
    return ResponseEntity.ok().body(inventoryCountService.scan(id, b));
  }

  @GetMapping("/{id}/results")
  public ResponseEntity<Object> results(@PathVariable String id) {
    return ResponseEntity.ok().body(inventoryCountService.getResults(id));
  }

  @PostMapping("/{id}/close")
  public ResponseEntity<Object> close(
      @PathVariable String id, @RequestBody(required = false) Map<String, Object> b) {
    return ResponseEntity.ok().body(inventoryCountService.close(id, b));
  }
}
