package sssm.dev.sssm_backend.a11_inventory.stock_movement.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a11_inventory.stock_movement.dto.StockMovementCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.stock_movement.dto.StockMovementUpdateRequest;
import sssm.dev.sssm_backend.a11_inventory.stock_movement.entity.StockMovement;
import sssm.dev.sssm_backend.a11_inventory.stock_movement.repository.StockMovementDAO;

@Service
@RequiredArgsConstructor
public class StockMovementService {
  private final StockMovementDAO stockMovementDAO;

  public List<StockMovement> getStockMovements(
      Optional<String> search, Optional<Integer> page, Optional<Integer> pageSize) {
    int pageNumber = page.orElse(1);
    int pageSizeNumber = pageSize.orElse(10);
    return search.isPresent()
        ? stockMovementDAO.searchStockMovementsByName(search.get(), pageNumber, pageSizeNumber)
        : stockMovementDAO.getAllStockMovements(pageNumber, pageSizeNumber);
  }

  public Optional<StockMovement> getStockMovementById(String stockMovementId) {
    Optional<StockMovement> stockMovement = stockMovementDAO.getStockMovementById(stockMovementId);
    if (stockMovement.isEmpty()) {
      throw new NotFoundException("StockMovement not found with ID: " + stockMovementId);
    }
    return Optional.of(stockMovement.get());
  }

  public StockMovement createStockMovement(StockMovementCreateRequest stockMovement) {
    return stockMovementDAO.createStockMovement(stockMovement);
  }

  public StockMovement updateStockMovement(
      String stockMovementId, StockMovementUpdateRequest updatedStockMovement) {
    if (stockMovementDAO.getStockMovementById(stockMovementId).isEmpty()) {
      throw new NotFoundException("StockMovement not found with ID: " + stockMovementId);
    }
    return stockMovementDAO.updateStockMovement(stockMovementId, updatedStockMovement);
  }

  public void deleteStockMovement(String stockMovementId) {
    stockMovementDAO.deleteStockMovement(stockMovementId);
  }
}
