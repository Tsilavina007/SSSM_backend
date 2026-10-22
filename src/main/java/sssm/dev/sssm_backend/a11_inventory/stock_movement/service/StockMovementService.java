package sssm.dev.sssm_backend.a11_inventory.stock_movement.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sssm.dev.sssm_backend.a00_infrastructure.exception.ClientException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ConflictException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a01_common.enums.ItemStatus;
import sssm.dev.sssm_backend.a01_common.enums.MovementType;
import sssm.dev.sssm_backend.a03_authentication.auth.security.CurrentUser;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemResponse;
import sssm.dev.sssm_backend.a11_inventory.item.repository.ItemDAO;
import sssm.dev.sssm_backend.a11_inventory.stock_balance.service.StockBalanceService;
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
  private final StockBalanceService stockBalanceService;
  private final CurrentUser currentUser;

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

  public List<StockMovementRest> getStockMovementsByItem(
      String itemId, int page, int pageSize) {
    return stockMovementDAO.getStockMovementsByItem(itemId, page, pageSize).stream()
        .map(stockMovementRestMapper::toRest)
        .toList();
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

    MovementType type =
        stockMovement.getMovement_type() != null
            ? stockMovement.getMovement_type()
            : MovementType.IN;
    BigDecimal quantity =
        stockMovement.getQuantity() == null ? BigDecimal.ONE : stockMovement.getQuantity();
    if (quantity.signum() <= 0) {
      throw new ClientException(
          "Quantité invalide",
          "La quantité d'un mouvement doit être strictement positive.",
          "CLIENT_ERROR");
    }

    if (type == MovementType.OUT && item.getStatus() != ItemStatus.AVAILABLE) {
      throw new ClientException(
          "Article non disponible",
          "L'article "
              + stockMovement.getItem_id()
              + " n'est pas disponible pour une sortie (statut: "
              + item.getStatus()
              + ").",
          "CLIENT_ERROR");
    }

    String source = trim(stockMovement.getSource_warehouse_id());
    String destination = trim(stockMovement.getDestination_warehouse_id());
    String warehouse = trim(stockMovement.getWarehouse_id());

    switch (type) {
      case IN, RETURN -> {
        if (destination == null) destination = warehouse;
        if (destination == null) {
          throw new ClientException(
              "Entrepôt de destination obligatoire",
              "Un mouvement d'entrée ou de retour doit préciser l'entrepôt de destination.",
              "CLIENT_ERROR");
        }
        stockMovement.setWarehouse_id(destination);
        stockMovement.setDestination_warehouse_id(destination);
      }
      case OUT -> {
        if (source == null) {
          source =
              item.getCurrent_warehouse_id() != null
                  ? item.getCurrent_warehouse_id()
                  : warehouse;
        }
        if (source == null) {
          throw new ClientException(
              "Entrepôt source obligatoire",
              "Une sortie doit préciser l'entrepôt source.",
              "CLIENT_ERROR");
        }
        stockMovement.setSource_warehouse_id(source);
      }
      case TRANSFER -> {
        if (source == null) source = item.getCurrent_warehouse_id();
        if (destination == null) destination = warehouse;
        if (source == null || destination == null) {
          throw new ClientException(
              "Entrepôts obligatoires",
              "Un transfert doit préciser l'entrepôt source et l'entrepôt de destination.",
              "CLIENT_ERROR");
        }
        if (source.equals(destination)) {
          throw new ClientException(
              "Transfert invalide",
              "L'entrepôt source et l'entrepôt de destination doivent être différents.",
              "CLIENT_ERROR");
        }
        stockMovement.setSource_warehouse_id(source);
        stockMovement.setDestination_warehouse_id(destination);
        stockMovement.setWarehouse_id(destination);
      }
      case DESTRUCTION -> {
        if (source == null) source = item.getCurrent_warehouse_id();
        stockMovement.setSource_warehouse_id(source);
      }
      case ADJUSTMENT -> {
        if (source == null && destination == null) {
          throw new ClientException(
              "Sens de l'ajustement obligatoire",
              "Un ajustement doit préciser un entrepôt source (diminution) ou un entrepôt de destination (augmentation).",
              "CLIENT_ERROR");
        }
        if (source != null && destination != null && source.equals(destination)) {
          throw new ClientException(
              "Ajustement invalide",
              "Les entrepôts source et destination d'un ajustement doivent être différents.",
              "CLIENT_ERROR");
        }
        stockMovement.setSource_warehouse_id(source);
        stockMovement.setDestination_warehouse_id(destination);
      }
    }

    stockMovement.setQuantity(quantity);
    stockMovement.setPerformed_by(resolvePerformedBy(stockMovement.getPerformed_by()));
    StockMovementResponse created = stockMovementDAO.createStockMovement(stockMovement);

    switch (type) {
      case IN, RETURN -> {
        stockBalanceService.adjust(
            item.getCompany_id(), destination, item.getItem_type_id(), quantity);
        itemDAO.updateItemStatus(stockMovement.getItem_id(), ItemStatus.AVAILABLE);
        itemDAO.updateItemLocation(stockMovement.getItem_id(), destination);
      }
      case OUT -> {
        stockBalanceService.adjust(
            item.getCompany_id(), source, item.getItem_type_id(), quantity.negate());
        itemDAO.updateItemStatus(stockMovement.getItem_id(), ItemStatus.ASSIGNED);
        itemDAO.updateItemLocation(stockMovement.getItem_id(), null);
      }
      case TRANSFER -> {
        stockBalanceService.adjust(
            item.getCompany_id(), source, item.getItem_type_id(), quantity.negate());
        stockBalanceService.adjust(
            item.getCompany_id(), destination, item.getItem_type_id(), quantity);
        itemDAO.updateItemLocation(stockMovement.getItem_id(), destination);
      }
      case DESTRUCTION -> {
        if (source != null) {
          stockBalanceService.adjust(
              item.getCompany_id(), source, item.getItem_type_id(), quantity.negate());
        }
        itemDAO.updateItemStatus(stockMovement.getItem_id(), ItemStatus.RETIRED);
        itemDAO.updateItemLocation(stockMovement.getItem_id(), null);
      }
      case ADJUSTMENT -> {
        if (source != null) {
          stockBalanceService.adjust(
              item.getCompany_id(), source, item.getItem_type_id(), quantity.negate());
        }
        if (destination != null) {
          stockBalanceService.adjust(
              item.getCompany_id(), destination, item.getItem_type_id(), quantity);
        }
        // L'ajustement corrige le solde ; il ne change pas l'état physique de l'article.
      }
    }

    return stockMovementRestMapper.toRest(created);
  }

  public StockMovementRest updateStockMovement(
      String stockMovementId, StockMovementUpdateRequest updatedStockMovement) {
    if (stockMovementDAO.getStockMovementById(stockMovementId).isEmpty()) {
      throw new NotFoundException(
          "Mouvement de stock introuvable",
          "Aucun mouvement de stock ne correspond à l'identifiant " + stockMovementId + ".",
          "NOT_FOUND");
    }
    throw new ConflictException(
        "Mouvement de stock immuable",
        "Un mouvement de stock validé ne peut pas être modifié. Créez un mouvement de compensation.",
        "IMMUTABLE_RESOURCE");
  }

  public void deleteStockMovement(String stockMovementId) {
    if (stockMovementDAO.getStockMovementById(stockMovementId).isEmpty()) {
      throw new NotFoundException(
          "Mouvement de stock introuvable",
          "Aucun mouvement de stock ne correspond à l'identifiant " + stockMovementId + ".",
          "NOT_FOUND");
    }
    throw new ConflictException(
        "Mouvement de stock immuable",
        "Un mouvement de stock validé ne peut pas être supprimé. Créez un mouvement de compensation.",
        "IMMUTABLE_RESOURCE");
  }

  private String resolvePerformedBy(String requested) {
    try {
      String userId = currentUser.require().userId();
      if (isUuid(userId)) return userId;
    } catch (RuntimeException ignored) {
      // Appel direct depuis un test ou contexte sans authentification : le champ demandé
      // reste utilisé s'il est valide.
    }
    return isUuid(requested) ? requested : null;
  }

  private static boolean isUuid(String value) {
    if (value == null || value.isBlank()) return false;
    try {
      UUID.fromString(value);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private static String trim(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
