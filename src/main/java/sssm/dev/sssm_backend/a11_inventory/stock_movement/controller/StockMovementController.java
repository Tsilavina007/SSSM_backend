package sssm.dev.sssm_backend.a11_inventory.stock_movement.controller;

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

import sssm.dev.sssm_backend.a11_inventory.stock_movement.dto.StockMovementCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.stock_movement.dto.StockMovementUpdateRequest;
import sssm.dev.sssm_backend.a11_inventory.stock_movement.dto.rest.StockMovementRest;
import sssm.dev.sssm_backend.a11_inventory.stock_movement.service.StockMovementService;

@RestController
@RequestMapping("/api/inventory/movements")
public class StockMovementController {
  private final StockMovementService stockMovementService;

  StockMovementController(StockMovementService stockMovementService) {
    this.stockMovementService = stockMovementService;
  }

  @GetMapping
  public ResponseEntity<Object> getStockMovements(
      @RequestParam Optional<Integer> page,
      @RequestParam Optional<Integer> pageSize,
      @RequestParam Optional<String> search) {
    List<StockMovementRest> stockMovements =
        stockMovementService.getStockMovements(search, page, pageSize);
    return ResponseEntity.ok().body(stockMovements);
  }

  @GetMapping("/{stockMovementId}")
  public ResponseEntity<Object> getStockMovementById(@PathVariable String stockMovementId) {
    Optional<StockMovementRest> stockMovement =
        stockMovementService.getStockMovementById(stockMovementId);
    return ResponseEntity.ok().body(stockMovement);
  }

  @PostMapping
  public ResponseEntity<Object> createStockMovement(
      @RequestBody StockMovementCreateRequest stockMovement) {
    StockMovementRest stockMovementRes = stockMovementService.createStockMovement(stockMovement);
    return ResponseEntity.ok().body(stockMovementRes);
  }

  @PatchMapping("/{stockMovementId}")
  public ResponseEntity<Object> updateStockMovement(
      @PathVariable String stockMovementId,
      @RequestBody StockMovementUpdateRequest updatedStockMovement) {
    StockMovementRest stockMovementRes =
        stockMovementService.updateStockMovement(stockMovementId, updatedStockMovement);
    return ResponseEntity.ok().body(stockMovementRes);
  }

  @DeleteMapping("/{stockMovementId}")
  public ResponseEntity<Object> deleteStockMovement(@PathVariable String stockMovementId) {
    stockMovementService.deleteStockMovement(stockMovementId);
    return ResponseEntity.ok().build();
  }
}
