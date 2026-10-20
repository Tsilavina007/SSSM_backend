package sssm.dev.sssm_backend.a11_inventory.stock_movement.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a11_inventory.stock_movement.dto.StockMovementCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.stock_movement.dto.StockMovementResponse;
import sssm.dev.sssm_backend.a11_inventory.stock_movement.dto.StockMovementUpdateRequest;
import sssm.dev.sssm_backend.a11_inventory.stock_movement.dto.mapper.StockMovementRestMapper;
import sssm.dev.sssm_backend.a11_inventory.stock_movement.dto.rest.StockMovementRest;
import sssm.dev.sssm_backend.a11_inventory.stock_movement.repository.StockMovementDAO;

@Service
@RequiredArgsConstructor
public class StockMovementService {
  private final StockMovementDAO stockMovementDAO;
  private final StockMovementRestMapper stockMovementRestMapper;

  public List<StockMovementRest> getStockMovements(
      Optional<String> search, Optional<Integer> page, Optional<Integer> pageSize) {
    int pageNumber = page.orElse(1);
    int pageSizeNumber = pageSize.orElse(10);
    List<StockMovementResponse> movements =
        search
            .map(
                value ->
                    stockMovementDAO.searchStockMovementsByName(value, pageNumber, pageSizeNumber))
            .orElseGet(() -> stockMovementDAO.getAllStockMovements(pageNumber, pageSizeNumber));
    return movements.stream().map(stockMovementRestMapper::toRest).toList();
  }

  public StockMovementRest getStockMovementById(String stockMovementId) {
    return stockMovementDAO
        .getStockMovementById(stockMovementId)
        .map(stockMovementRestMapper::toRest)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Mouvement de stock introuvable",
                    "Aucun mouvement de stock ne correspond à l'identifiant "
                        + stockMovementId
                        + ".",
                    "NOT_FOUND"));
  }

  public StockMovementRest createStockMovement(StockMovementCreateRequest stockMovement) {
    return stockMovementRestMapper.toRest(stockMovementDAO.createStockMovement(stockMovement));
  }

  public StockMovementRest updateStockMovement(
      String stockMovementId, StockMovementUpdateRequest updatedStockMovement) {
    if (stockMovementDAO.getStockMovementById(stockMovementId).isEmpty()) {
      throw new NotFoundException(
          "Mouvement de stock introuvable",
          "Aucun mouvement de stock ne correspond à l'identifiant " + stockMovementId + ".",
          "NOT_FOUND");
    }
    return stockMovementRestMapper.toRest(
        stockMovementDAO.updateStockMovement(stockMovementId, updatedStockMovement));
  }

  public void deleteStockMovement(String stockMovementId) {
    stockMovementDAO.deleteStockMovement(stockMovementId);
  }
}
