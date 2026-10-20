package sssm.dev.sssm_backend.a11_inventory.stock_movement.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a01_common.enums.ItemStatus;
import sssm.dev.sssm_backend.a01_common.enums.MovementType;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemResponse;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemUpdateRequest;
import sssm.dev.sssm_backend.a11_inventory.item.repository.ItemDAO;
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
  private final ItemDAO itemDAO;
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

  @Transactional
  public StockMovementRest createStockMovement(StockMovementCreateRequest stockMovement) {
    // Vérifier que l'item existe
    ItemResponse item =
        itemDAO
            .getItemById(stockMovement.getItem_id())
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Article introuvable",
                        "Aucun article ne correspond à l'identifiant "
                            + stockMovement.getItem_id()
                            + ".",
                        "NOT_FOUND"));

    // Vérifier la disponibilité pour les sorties
    MovementType type =
        stockMovement.getMovement_type() != null
            ? stockMovement.getMovement_type()
            : MovementType.IN;
    if (type == MovementType.OUT && item.getStatus() != ItemStatus.AVAILABLE) {
      throw new sssm.dev.sssm_backend.a00_infrastructure.exception.ClientException(
          "Article non disponible",
          "L'article "
              + stockMovement.getItem_id()
              + " n'est pas disponible pour une sortie (statut: "
              + item.getStatus()
              + ").",
          "CLIENT_ERROR");
    }

    // Créer le mouvement
    StockMovementRest result =
        stockMovementRestMapper.toRest(stockMovementDAO.createStockMovement(stockMovement));

    // Mettre à jour l'état de l'item selon le type de mouvement
    ItemUpdateRequest update = new ItemUpdateRequest();
    switch (type) {
      case IN:
        update.setStatus(ItemStatus.AVAILABLE);
        update.setCurrent_warehouse_id(stockMovement.getWarehouse_id());
        break;
      case OUT:
        update.setStatus(ItemStatus.ASSIGNED);
        update.setCurrent_warehouse_id(null);
        break;
      case TRANSFER:
        update.setCurrent_warehouse_id(stockMovement.getWarehouse_id());
        break;
      case RETURN:
        update.setStatus(ItemStatus.AVAILABLE);
        update.setCurrent_warehouse_id(stockMovement.getWarehouse_id());
        break;
      case DESTRUCTION:
        update.setStatus(ItemStatus.RETIRED);
        update.setCurrent_warehouse_id(null);
        break;
      case ADJUSTMENT:
        // Pas de mise à jour automatique
        break;
    }
    if (update.getStatus() != null || update.getCurrent_warehouse_id() != null) {
      itemDAO.updateItem(stockMovement.getItem_id(), update);
    }

    return result;
  }

  @Transactional
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

  @Transactional
  public void deleteStockMovement(String stockMovementId) {
    stockMovementDAO.deleteStockMovement(stockMovementId);
  }
}
