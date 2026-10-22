package sssm.dev.sssm_backend.a11_inventory.operation.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sssm.dev.sssm_backend.a00_infrastructure.exception.ClientException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;
import sssm.dev.sssm_backend.a01_common.enums.MovementType;
import sssm.dev.sssm_backend.a01_common.errors.ItemError;
import sssm.dev.sssm_backend.a11_inventory.operation.dto.OperationLineRequest;
import sssm.dev.sssm_backend.a11_inventory.operation.dto.OperationRequest;
import sssm.dev.sssm_backend.a11_inventory.operation.dto.OperationResponse;
import sssm.dev.sssm_backend.a11_inventory.operation.dto.mapper.OperationRestMapper;
import sssm.dev.sssm_backend.a11_inventory.operation.repository.OperationDAO;
import sssm.dev.sssm_backend.a11_inventory.stock_movement.dto.StockMovementCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.stock_movement.dto.rest.StockMovementRest;
import sssm.dev.sssm_backend.a11_inventory.stock_movement.service.StockMovementService;

@Service
@RequiredArgsConstructor
public class OperationService {
  private static final Set<String> TYPES =
      Set.of("RECEIPT", "ISSUE", "TRANSFER", "RETURN", "ADJUSTMENT", "ASSIGNMENT");

  private final OperationDAO operationDAO;
  private final OperationRestMapper operationRestMapper;
  private final StockMovementService stockMovementService;

  public List<Map<String, Object>> list(Integer p, Integer s, String q, String w, Object... a) {
    return operationDAO.list(p, s, q, w, a);
  }

  public Object one(String id) {
    OperationResponse r = operationDAO.one(id).orElse(null);
    return r != null
        ? operationRestMapper.toRest(r)
        : Map.of(
            "errors", new ItemError("Opération d'inventaire introuvable", "Aucune opération d'inventaire ne correspond à l'identifiant " + id + ".", "NOT_FOUND"));
  }

  /**
   * Crée une opération complète : en-tête, lignes et mouvements dans la même transaction.
   * Le type est imposé par la route ; un corps sans ligne est refusé.
   */
  @Transactional
  public Object create(String operationType, OperationRequest request) {
    String type = operationType == null ? null : operationType.trim().toUpperCase();
    if (type == null || !TYPES.contains(type)) {
      throw new ClientException(
          "Type d'opération invalide",
          "Le type d'opération doit valoir RECEIPT, ISSUE, TRANSFER, RETURN, ADJUSTMENT ou ASSIGNMENT.",
          "CLIENT_ERROR");
    }
    if (request.getCompany_id() == null || request.getCompany_id().isBlank()) {
      throw new ClientException(
          "Entreprise obligatoire",
          "Le champ 'company_id' est obligatoire pour créer une opération.",
          "CLIENT_ERROR");
    }
    List<OperationLineRequest> lines = request.getLines();
    if (lines == null || lines.isEmpty()) {
      throw new ClientException(
          "Ligne d'article obligatoire",
          "Une opération doit contenir au moins une ligne d'article.",
          "CLIENT_ERROR");
    }

    Map<String, Object> header = new LinkedHashMap<>();
    header.put("company_id", request.getCompany_id());
    header.put("operation_type", type);
    header.put("status", "COMPLETED");
    put(header, "reference", request.getReference());
    put(header, "idempotency_key", request.getIdempotency_key());
    put(header, "source_warehouse_id", request.getSource_warehouse_id());
    put(header, "destination_warehouse_id", request.getDestination_warehouse_id());
    put(header, "employee_id", request.getEmployee_id());
    put(header, "post_id", request.getPost_id());
    put(header, "performed_by", request.getPerformed_by());
    put(header, "notes", request.getNotes());
    header.put("completed_at", Timestamp.from(Instant.now()));

    Object saved = operationDAO.save(null, header, false);
    if (!(saved instanceof OperationResponse operation)) {
      return saved;
    }

    for (OperationLineRequest line : lines) {
      if (line == null || line.getItem_id() == null || line.getItem_id().isBlank()) {
        throw new ClientException(
            "Article obligatoire",
            "Chaque ligne d'opération doit préciser un 'item_id'.",
            "CLIENT_ERROR");
      }
      BigDecimal quantity = line.getQuantity() == null ? BigDecimal.ONE : line.getQuantity();
      if (quantity.signum() <= 0) {
        throw new ClientException(
            "Quantité invalide",
            "La quantité d'une ligne d'opération doit être strictement positive.",
            "CLIENT_ERROR");
      }

      String source = firstNonBlank(line.getSource_warehouse_id(), request.getSource_warehouse_id());
      String destination =
          firstNonBlank(line.getDestination_warehouse_id(), request.getDestination_warehouse_id());
      MovementType movementType = movementType(type);
      String warehouse = warehouseFor(type, source, destination);
      validateRouteWarehouses(type, source, destination);

      StockMovementCreateRequest movement = new StockMovementCreateRequest();
      movement.setWarehouse_id(warehouse);
      movement.setItem_id(line.getItem_id());
      movement.setMovement_type(movementType);
      movement.setQuantity(quantity);
      movement.setSource_warehouse_id(source);
      movement.setDestination_warehouse_id(destination);
      movement.setOperation_id(operation.getId());
      movement.setPerformed_by(request.getPerformed_by());
      movement.setReference(request.getReference());
      movement.setNotes(line.getNotes() == null ? request.getNotes() : line.getNotes());

      StockMovementRest created = stockMovementService.createStockMovement(movement);
      operationDAO.addLine(
          operation.getId(),
          line.getItem_id(),
          created.getId(),
          quantity,
          line.getNotes());
    }

    return operationDAO
        .one(operation.getId())
        .map(operationRestMapper::toRest)
        .orElseThrow(
            () ->
                new ServerException(
                    "Opération d'inventaire introuvable après création.",
                    "L'opération créée n'a pas pu être relue depuis la base.",
                    "SERVER_ERROR"));
  }

  @Transactional
  public Object save(String id, Map<String, Object> b, boolean u) {
    Object r = operationDAO.save(id, b, u);
    return r instanceof OperationResponse rs ? operationRestMapper.toRest(rs) : r;
  }

  public Map<String, Object> delete(String id) {
    return operationDAO.delete(id);
  }

  public List<Map<String, Object>> query(String q, Object... a) {
    return operationDAO.query(q, a);
  }

  private static MovementType movementType(String operationType) {
    return switch (operationType) {
      case "RECEIPT" -> MovementType.IN;
      case "ISSUE", "ASSIGNMENT" -> MovementType.OUT;
      case "TRANSFER" -> MovementType.TRANSFER;
      case "RETURN" -> MovementType.RETURN;
      case "ADJUSTMENT" -> MovementType.ADJUSTMENT;
      default -> throw new ClientException("Type d'opération invalide", operationType, "CLIENT_ERROR");
    };
  }

  private static String warehouseFor(String type, String source, String destination) {
    return switch (type) {
      case "RECEIPT", "RETURN", "TRANSFER" -> destination;
      case "ISSUE", "ASSIGNMENT" -> source;
      default -> destination != null ? destination : source;
    };
  }

  private static void validateRouteWarehouses(
      String type, String source, String destination) {
    switch (type) {
      case "RECEIPT", "RETURN" -> {
        if (destination == null) {
          throw new ClientException(
              "Entrepôt de destination obligatoire",
              "Cette opération doit préciser un entrepôt de destination.",
              "CLIENT_ERROR");
        }
      }
      case "ISSUE", "ASSIGNMENT" -> {
        if (source == null) {
          throw new ClientException(
              "Entrepôt source obligatoire",
              "Cette opération doit préciser un entrepôt source.",
              "CLIENT_ERROR");
        }
      }
      case "TRANSFER" -> {
        if (source == null || destination == null) {
          throw new ClientException(
              "Entrepôts obligatoires",
              "Un transfert doit préciser un entrepôt source et un entrepôt de destination.",
              "CLIENT_ERROR");
        }
        if (source.equals(destination)) {
          throw new ClientException(
              "Transfert invalide",
              "Les entrepôts source et destination doivent être différents.",
              "CLIENT_ERROR");
        }
      }
      case "ADJUSTMENT" -> {
        if (source == null && destination == null) {
          throw new ClientException(
              "Sens de l'ajustement obligatoire",
              "Un ajustement doit préciser un entrepôt source ou destination.",
              "CLIENT_ERROR");
        }
      }
    }
  }

  private static String firstNonBlank(String first, String second) {
    if (first != null && !first.isBlank()) return first.trim();
    return second == null || second.isBlank() ? null : second.trim();
  }

  private static void put(Map<String, Object> values, String key, Object value) {
    if (value != null) values.put(key, value);
  }
}
