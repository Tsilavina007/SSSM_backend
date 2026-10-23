package sssm.dev.sssm_backend.a11_inventory.inventory_count.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sssm.dev.sssm_backend.a01_common.pagination.PageResponse;
import sssm.dev.sssm_backend.a11_inventory.inventory_count.dto.InventoryCountCloseRequest;
import sssm.dev.sssm_backend.a11_inventory.inventory_count.dto.InventoryCountCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.inventory_count.dto.InventoryCountScanRequest;
import sssm.dev.sssm_backend.a11_inventory.inventory_count.dto.rest.InventoryCountRest;
import sssm.dev.sssm_backend.a11_inventory.inventory_count.dto.rest.InventoryCountScanRest;
import sssm.dev.sssm_backend.a11_inventory.inventory_count.service.InventoryCountService;

@RestController
@RequestMapping("/api/v1/inventory/inventory-counts")
public class InventoryCountController {
  private static final int DEFAULT_PAGE_SIZE = 50;

  private final InventoryCountService inventoryCountService;

  InventoryCountController(InventoryCountService inventoryCountService) {
    this.inventoryCountService = inventoryCountService;
  }

  @GetMapping
  public PageResponse<InventoryCountRest> list(
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer pageSize) {
    return inventoryCountService.list(page, pageSize, null);
  }

  @PostMapping
  public ResponseEntity<Object> create(@RequestBody InventoryCountCreateRequest request) {
    return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
        .body(inventoryCountService.create(request));
  }

  @GetMapping("/{id}")
  public InventoryCountRest one(@PathVariable String id) {
    return inventoryCountService.one(id);
  }

  @PostMapping("/{id}/scans")
  public InventoryCountScanRest scan(
      @PathVariable String id, @RequestBody InventoryCountScanRequest request) {
    return inventoryCountService.scan(id, request.toMap());
  }

  /**
   * Lignes paginées du comptage. Le total est exposé dans l'en-tête {@code X-Total-Count} pour
   * rester compatible avec la réponse tableau existante.
   */
  @GetMapping("/{id}/results")
  public ResponseEntity<PageResponse<InventoryCountScanRest>> results(
      @PathVariable String id,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer pageSize) {
    PageResponse<InventoryCountScanRest> lines =
        inventoryCountService.getResults(
            id,
            page == null ? 1 : page,
            pageSize == null ? DEFAULT_PAGE_SIZE : pageSize);
    return ResponseEntity.ok()
        .header("X-Total-Count", String.valueOf(lines.total()))
        .header("X-Page", String.valueOf(lines.page()))
        .header("X-Page-Size", String.valueOf(lines.pageSize()))
        .body(lines);
  }

  @PostMapping("/{id}/close")
  public ResponseEntity<Object> close(
      @PathVariable String id, @RequestBody(required = false) InventoryCountCloseRequest request) {
    return ResponseEntity.ok()
        .body(inventoryCountService.close(id, request == null ? null : request.toMap()));
  }

  /** Annule un comptage en cours, sans ajuster les soldes ni les articles. */
  @PostMapping("/{id}/cancel")
  public ResponseEntity<Object> cancel(@PathVariable String id) {
    return ResponseEntity.ok().body(inventoryCountService.cancel(id));
  }
}
