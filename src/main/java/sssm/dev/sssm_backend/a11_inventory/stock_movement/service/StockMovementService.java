package sssm.dev.sssm_backend.a11_inventory.stock_movement.service;

import java.util.List;
import java.util.Optional;

import sssm.dev.sssm_backend.a11_inventory.stock_movement.entity.StockMovement;
import sssm.dev.sssm_backend.a11_inventory.stock_movement.repository.StockMovementDAO;
import sssm.dev.sssm_backend.a11_inventory.stock_movement.repository.StockMovementDAOImpl;

public class StockMovementService {
  private final StockMovementDAO stockMovementDAO;

  public StockMovementService() {
    this.stockMovementDAO = new StockMovementDAOImpl();
  }

  public List<StockMovement> getStockMovements(
      Optional<String> search, Optional<Integer> page, Optional<Integer> pageSize) {
    int pageNumber = page.orElse(1);
    int pageSizeNumber = pageSize.orElse(10);
    return search.isPresent()
        ? stockMovementDAO.searchStockMovementsByName(search.get(), pageNumber, pageSizeNumber)
        : stockMovementDAO.getAllStockMovements(pageNumber, pageSizeNumber);
  }

  public Optional<StockMovement> getStockMovementById(String stockMovementId) {
    return stockMovementDAO.getStockMovementById(stockMovementId);
  }

  public StockMovement createStockMovement(StockMovement stockMovement) {
    return stockMovementDAO.createStockMovement(stockMovement);
  }

  public StockMovement updateStockMovement(
      String stockMovementId, StockMovement updatedStockMovement) {
    if (stockMovementDAO.getStockMovementById(stockMovementId).isEmpty()) {
      throw new IllegalArgumentException("StockMovement not found with ID: " + stockMovementId);
    }
    updatedStockMovement.setId(stockMovementId);
    return stockMovementDAO.updateStockMovement(updatedStockMovement);
  }

  public void deleteStockMovement(String stockMovementId) {
    stockMovementDAO.deleteStockMovement(stockMovementId);
  }
}
