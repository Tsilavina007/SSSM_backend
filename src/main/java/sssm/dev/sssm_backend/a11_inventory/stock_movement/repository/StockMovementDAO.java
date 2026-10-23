package sssm.dev.sssm_backend.a11_inventory.stock_movement.repository;

import java.util.List;
import java.util.Optional;

import sssm.dev.sssm_backend.a11_inventory.stock_movement.dto.StockMovementCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.stock_movement.dto.StockMovementResponse;
import sssm.dev.sssm_backend.a11_inventory.stock_movement.dto.StockMovementUpdateRequest;

public interface StockMovementDAO {
  List<StockMovementResponse> getAllStockMovements(int page, int pageSize);

  List<StockMovementResponse> searchStockMovementsByName(String value, int page, int pageSize);

  List<StockMovementResponse> getStockMovementsByItem(String itemId, int page, int pageSize);

  List<StockMovementResponse> getStockMovementsByItem(
      String itemId, String movementType, int page, int pageSize);

  long countStockMovementsByItem(String itemId, String movementType);

  long countStockMovements(String search);

  long countStockMovementsByItem(String itemId);

  Optional<StockMovementResponse> getStockMovementById(String id);

  StockMovementResponse createStockMovement(StockMovementCreateRequest value);

  StockMovementResponse updateStockMovement(String id, StockMovementUpdateRequest value);

  void deleteStockMovement(String id);
}
