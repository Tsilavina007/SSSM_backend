package sssm.dev.sssm_backend.a11_inventory.stock_movement.repository;

import java.util.List;
import java.util.Optional;

import sssm.dev.sssm_backend.a11_inventory.stock_movement.entity.StockMovement;

public interface StockMovementDAO {
  List<StockMovement> getAllStockMovements(int page, int pageSize);

  List<StockMovement> searchStockMovementsByName(String value, int page, int pageSize);

  Optional<StockMovement> getStockMovementById(String id);

  StockMovement createStockMovement(StockMovement value);

  StockMovement updateStockMovement(StockMovement value);

  void deleteStockMovement(String id);
}
