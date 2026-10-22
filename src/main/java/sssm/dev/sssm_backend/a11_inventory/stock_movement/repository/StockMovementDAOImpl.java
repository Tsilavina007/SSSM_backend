package sssm.dev.sssm_backend.a11_inventory.stock_movement.repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;
import sssm.dev.sssm_backend.a01_common.enums.MovementType;
import sssm.dev.sssm_backend.a11_inventory.stock_movement.dto.StockMovementCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.stock_movement.dto.StockMovementResponse;
import sssm.dev.sssm_backend.a11_inventory.stock_movement.dto.StockMovementUpdateRequest;

@Repository
public class StockMovementDAOImpl implements StockMovementDAO {

  @Override
  public List<StockMovementResponse> getAllStockMovements(int page, int pageSize) {
    String sql = "SELECT * FROM security.stock_movement ORDER BY id LIMIT ? OFFSET ?";
    List<StockMovementResponse> values = new ArrayList<>();
    int offset = (page - 1) * pageSize;

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, pageSize);
      stmt.setInt(2, offset);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          values.add(mapRowToStockMovementResponse(rs));
        }
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return values;
  }

  @Override
  public List<StockMovementResponse> searchStockMovementsByName(
      String value, int page, int pageSize) {
    String sql =
        "SELECT * FROM security.stock_movement WHERE reference ILIKE ? OR notes ILIKE ? ORDER BY id LIMIT ? OFFSET ?";
    List<StockMovementResponse> values = new ArrayList<>();
    int offset = (page - 1) * pageSize;

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      String searchPattern = "%" + value + "%";
      stmt.setString(1, searchPattern);
      stmt.setString(2, searchPattern);
      stmt.setInt(3, pageSize);
      stmt.setInt(4, offset);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          values.add(mapRowToStockMovementResponse(rs));
        }
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return values;
  }

  @Override
  public List<StockMovementResponse> getStockMovementsByItem(
      String itemId, int page, int pageSize) {
    String sql =
        "SELECT * FROM security.stock_movement WHERE item_id = ? ORDER BY id LIMIT ? OFFSET ?";
    List<StockMovementResponse> values = new ArrayList<>();
    int offset = (page - 1) * pageSize;

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, UUID.fromString(itemId));
      stmt.setInt(2, pageSize);
      stmt.setInt(3, offset);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          values.add(mapRowToStockMovementResponse(rs));
        }
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return values;
  }

  @Override
  public Optional<StockMovementResponse> getStockMovementById(String id) {
    String sql = "SELECT * FROM security.stock_movement WHERE id = ?";
    UUID uuid = UUID.fromString(id);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, uuid);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapRowToStockMovementResponse(rs));
        }
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return Optional.empty();
  }

  @Override
  public StockMovementResponse createStockMovement(StockMovementCreateRequest value) {
    // Colonnes requises toujours écrites ; les colonnes nullable sont omises
    // quand absentes pour laisser s'appliquer les défauts PostgreSQL
    // (movement_date NOT NULL DEFAULT now()).
    StringBuilder columns = new StringBuilder("warehouse_id, item_id, movement_type, quantity");
    StringBuilder placeholders =
        new StringBuilder("?, ?, CAST(? AS security.movement_type), ?");
    List<Object> parameters = new ArrayList<>();
    String generatedId = null;

    parameters.add(
        value.getWarehouse_id() == null ? null : UUID.fromString(value.getWarehouse_id()));
    parameters.add(value.getItem_id() == null ? null : UUID.fromString(value.getItem_id()));
    parameters.add(
        value.getMovement_type() == null
            ? MovementType.IN.name()
            : value.getMovement_type().name());
    parameters.add(value.getQuantity() == null ? BigDecimal.ONE : value.getQuantity());

    if (value.getEmployee_id() != null) {
      columns.append(", employee_id");
      placeholders.append(", ?");
      parameters.add(UUID.fromString(value.getEmployee_id()));
    }
    if (value.getSource_warehouse_id() != null) {
      columns.append(", source_warehouse_id");
      placeholders.append(", ?");
      parameters.add(UUID.fromString(value.getSource_warehouse_id()));
    }
    if (value.getDestination_warehouse_id() != null) {
      columns.append(", destination_warehouse_id");
      placeholders.append(", ?");
      parameters.add(UUID.fromString(value.getDestination_warehouse_id()));
    }
    if (value.getOperation_id() != null) {
      columns.append(", operation_id");
      placeholders.append(", ?");
      parameters.add(UUID.fromString(value.getOperation_id()));
    }
    if (value.getPerformed_by() != null) {
      columns.append(", performed_by");
      placeholders.append(", ?");
      parameters.add(UUID.fromString(value.getPerformed_by()));
    }
    if (value.getReference() != null) {
      columns.append(", reference");
      placeholders.append(", ?");
      parameters.add(value.getReference());
    }
    if (value.getMovement_date() != null) {
      columns.append(", movement_date");
      placeholders.append(", ?");
      parameters.add(value.getMovement_date());
    }
    if (value.getNotes() != null) {
      columns.append(", notes");
      placeholders.append(", ?");
      parameters.add(value.getNotes());
    }

    String sql =
        "INSERT INTO security.stock_movement ("
            + columns
            + ") VALUES ("
            + placeholders
            + ") RETURNING id";

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      for (int index = 0; index < parameters.size(); index++) {
        stmt.setObject(index + 1, parameters.get(index));
      }

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          generatedId = rs.getString("id");
        }
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }

    if (generatedId == null) {
      throw new ServerException("La base n'a pas renvoyé l'identifiant du mouvement créé.");
    }
    // Relire la ligne persistée pour renvoyer les valeurs calculées par PostgreSQL
    // (movement_date par défaut, type par défaut).
    return getStockMovementById(generatedId)
        .orElseThrow(
            () ->
                new ServerException(
                    "Le mouvement créé n'a pas pu être relu depuis la base."));
  }

  @Override
  public StockMovementResponse updateStockMovement(String id, StockMovementUpdateRequest value) {
    StringBuilder sql = new StringBuilder("UPDATE security.stock_movement SET ");
    List<Object> parameters = new ArrayList<>();

    if (value.getWarehouse_id() != null) {
      sql.append("warehouse_id = ?, ");
      parameters.add(UUID.fromString(value.getWarehouse_id()));
    }
    if (value.getItem_id() != null) {
      sql.append("item_id = ?, ");
      parameters.add(UUID.fromString(value.getItem_id()));
    }
    if (value.getEmployee_id() != null) {
      sql.append("employee_id = ?, ");
      parameters.add(UUID.fromString(value.getEmployee_id()));
    }
    if (value.getMovement_type() != null) {
      sql.append("movement_type = CAST(? AS security.movement_type), ");
      parameters.add(value.getMovement_type().name());
    }
    if (value.getReference() != null) {
      sql.append("reference = ?, ");
      parameters.add(value.getReference());
    }
    if (value.getMovement_date() != null) {
      sql.append("movement_date = ?, ");
      parameters.add(value.getMovement_date());
    }
    if (value.getNotes() != null) {
      sql.append("notes = ?, ");
      parameters.add(value.getNotes());
    }

    if (parameters.isEmpty()) {
      return getStockMovementById(id)
          .orElseThrow(
              () ->
                  new NotFoundException(
                      "Mouvement de stock introuvable",
                      "Aucun mouvement de stock ne correspond à l'identifiant " + id + ".",
                      "NOT_FOUND"));
    }

    sql.setLength(sql.length() - 2);
    sql.append(" WHERE id = ?");
    parameters.add(UUID.fromString(id));

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
      for (int index = 0; index < parameters.size(); index++) {
        stmt.setObject(index + 1, parameters.get(index));
      }
      stmt.executeUpdate();
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return getStockMovementById(id)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Mouvement de stock introuvable",
                    "Aucun mouvement de stock ne correspond à l'identifiant " + id + ".",
                    "NOT_FOUND"));
  }

  @Override
  public void deleteStockMovement(String id) {
    String sql = "DELETE FROM security.stock_movement WHERE id = ?";
    UUID uuid = UUID.fromString(id);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, uuid);
      stmt.executeUpdate();
    } catch (SQLException e) {
      throw new ServerException(e);
    }
  }

  private StockMovementResponse mapRowToStockMovementResponse(ResultSet rs) throws SQLException {
    return new StockMovementResponse(
        rs.getString("id"),
        rs.getString("warehouse_id"),
        rs.getString("item_id"),
        rs.getString("employee_id"),
        MovementType.valueOf(rs.getString("movement_type")),
        rs.getString("reference"),
        rs.getTimestamp("movement_date"),
        rs.getString("notes"),
        rs.getString("source_warehouse_id"),
        rs.getString("destination_warehouse_id"),
        rs.getString("operation_id"),
        rs.getBigDecimal("quantity"),
        rs.getString("performed_by"),
        rs.getTimestamp("created_at"));
  }
}
