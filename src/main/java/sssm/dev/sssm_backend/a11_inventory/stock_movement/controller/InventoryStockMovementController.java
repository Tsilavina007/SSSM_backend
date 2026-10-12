package sssm.dev.sssm_backend.a11_inventory.stock_movement.controller;

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

import sssm.dev.sssm_backend.a11_inventory.stock_movement.entity.StockMovement;
import sssm.dev.sssm_backend.a11_inventory.stock_movement.service.StockMovementService;

@RestController
@RequestMapping("/api/inventory/movements")
public class InventoryStockMovementController {
  private final StockMovementService stockMovementService;

  public InventoryStockMovementController() {
    this.stockMovementService = new StockMovementService();
  }

  @GetMapping
  public List<StockMovement> getStockMovements(
      @RequestParam Optional<Integer> page,
      @RequestParam Optional<Integer> pageSize,
      @RequestParam Optional<String> search) {
    return stockMovementService.getStockMovements(search, page, pageSize);
  }

  @GetMapping("/{stockMovementId}")
  public Optional<StockMovement> getStockMovementById(@PathVariable String stockMovementId) {
    return stockMovementService.getStockMovementById(stockMovementId);
  }

  @PostMapping
  public StockMovement createStockMovement(@RequestBody StockMovement stockMovement) {
    return stockMovementService.createStockMovement(stockMovement);
  }

  @PatchMapping("/{stockMovementId}")
  public StockMovement updateStockMovement(
      @PathVariable String stockMovementId, @RequestBody StockMovement updatedStockMovement) {
    return stockMovementService.updateStockMovement(stockMovementId, updatedStockMovement);
  }

  @DeleteMapping("/{stockMovementId}")
  public void deleteStockMovement(@PathVariable String stockMovementId) {
    stockMovementService.deleteStockMovement(stockMovementId);
  }
}
