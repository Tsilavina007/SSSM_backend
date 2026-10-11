package sssm.dev.sssm_backend.a11_inventory.stock_movement.service;

import java.util.*;
import sssm.dev.sssm_backend.a11_inventory.stock_movement.entity.StockMovement;
import sssm.dev.sssm_backend.a11_inventory.stock_movement.repository.*;

public class StockMovementService {
  private final StockMovementDAO dao = new StockMovementDAOImpl();

  public List<StockMovement> findAll(Optional<String> q, Optional<Integer> p, Optional<Integer> s) {
    int page = p.orElse(1), size = s.orElse(10);
    return q.isPresent() ? dao.search(q.get(), page, size) : dao.findAll(page, size);
  }

  public Optional<StockMovement> findById(String id) {
    return dao.findById(id);
  }

  public StockMovement create(StockMovement v) {
    return dao.create(v);
  }

  public StockMovement update(String id, StockMovement v) {
    if (dao.findById(id).isEmpty())
      throw new IllegalArgumentException("StockMovement not found with ID: " + id);
    v.setId(id);
    return dao.update(v);
  }

  public void delete(String id) {
    dao.delete(id);
  }
}
