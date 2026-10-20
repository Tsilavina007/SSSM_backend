package sssm.dev.sssm_backend.a11_inventory.stock_movement.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
  public List<StockMovementRest> getStockMovements(
      @RequestParam Optional<Integer> page,
      @RequestParam Optional<Integer> pageSize,
      @RequestParam Optional<String> search) {
    return stockMovementService.getStockMovements(search, page, pageSize);
  }

  @GetMapping("/{stockMovementId}")
  public StockMovementRest getStockMovementById(@PathVariable String stockMovementId) {
    return stockMovementService.getStockMovementById(stockMovementId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public StockMovementRest createStockMovement(
      @Valid @RequestBody StockMovementCreateRequest stockMovement) {
    return stockMovementService.createStockMovement(stockMovement);
  }

  @PatchMapping("/{stockMovementId}")
  public StockMovementRest updateStockMovement(
      @PathVariable String stockMovementId,
      @Valid @RequestBody StockMovementUpdateRequest updatedStockMovement) {
    return stockMovementService.updateStockMovement(stockMovementId, updatedStockMovement);
  }

  @DeleteMapping("/{stockMovementId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteStockMovement(@PathVariable String stockMovementId) {
    stockMovementService.deleteStockMovement(stockMovementId);
  }
}
