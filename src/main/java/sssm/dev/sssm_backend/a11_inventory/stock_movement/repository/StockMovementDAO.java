package sssm.dev.sssm_backend.a11_inventory.stock_movement.repository;

import java.util.*;
import sssm.dev.sssm_backend.a11_inventory.stock_movement.entity.StockMovement;

public interface StockMovementDAO {
  List<StockMovement> findAll(int page, int pageSize);

  List<StockMovement> search(String value, int page, int pageSize);

  Optional<StockMovement> findById(String id);

  StockMovement create(StockMovement value);

  StockMovement update(StockMovement value);

  void delete(String id);
}
