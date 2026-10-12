package sssm.dev.sssm_backend.a11_inventory.stock_movement.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a01_common.enums.MovementType;
import sssm.dev.sssm_backend.a11_inventory.stock_movement.entity.StockMovement;

public class StockMovementDAOImpl implements StockMovementDAO {

  @Override
  public List<StockMovement> getAllStockMovements(int page, int pageSize) {
    String sql = "SELECT * FROM security.stock_movement ORDER BY id LIMIT ? OFFSET ?";
    List<StockMovement> values = new ArrayList<>();
    int offset = (page - 1) * pageSize;

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, pageSize);
      stmt.setInt(2, offset);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          values.add(
              new StockMovement(
                  rs.getString("id"),
                  rs.getString("warehouse_id"),
                  rs.getString("item_id"),
                  rs.getString("employee_id"),
                  MovementType.valueOf(rs.getString("movement_type")),
                  rs.getString("reference"),
                  rs.getTimestamp("movement_date"),
                  rs.getString("notes")));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return values;
  }

  @Override
  public List<StockMovement> searchStockMovementsByName(String value, int page, int pageSize) {
    String sql =
        "SELECT * FROM security.stock_movement WHERE reference ILIKE ? OR notes ILIKE ? ORDER BY id LIMIT ? OFFSET ?";
    List<StockMovement> values = new ArrayList<>();
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
          values.add(
              new StockMovement(
                  rs.getString("id"),
                  rs.getString("warehouse_id"),
                  rs.getString("item_id"),
                  rs.getString("employee_id"),
                  MovementType.valueOf(rs.getString("movement_type")),
                  rs.getString("reference"),
                  rs.getTimestamp("movement_date"),
                  rs.getString("notes")));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return values;
  }

  @Override
  public Optional<StockMovement> getStockMovementById(String id) {
    String sql = "SELECT * FROM security.stock_movement WHERE id = ?";
    UUID uuid = UUID.fromString(id);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, uuid);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(
              new StockMovement(
                  rs.getString("id"),
                  rs.getString("warehouse_id"),
                  rs.getString("item_id"),
                  rs.getString("employee_id"),
                  MovementType.valueOf(rs.getString("movement_type")),
                  rs.getString("reference"),
                  rs.getTimestamp("movement_date"),
                  rs.getString("notes")));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }

  @Override
  public StockMovement createStockMovement(StockMovement value) {
    String sql =
        "INSERT INTO security.stock_movement (warehouse_id, item_id, employee_id, movement_type, reference, movement_date, notes) VALUES (?, ?, ?, CAST(? AS movement_type), ?, ?, ?)";

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      stmt.setObject(
          1, value.getWarehouse_id() == null ? null : UUID.fromString(value.getWarehouse_id()));
      stmt.setObject(2, value.getItem_id() == null ? null : UUID.fromString(value.getItem_id()));
      stmt.setObject(
          3, value.getEmployee_id() == null ? null : UUID.fromString(value.getEmployee_id()));
      stmt.setString(
          4,
          value.getMovement_type() == null
              ? MovementType.IN.name()
              : value.getMovement_type().name());
      stmt.setString(5, value.getReference());
      stmt.setTimestamp(6, value.getMovement_date());
      stmt.setString(7, value.getNotes());
      stmt.executeUpdate();

      try (ResultSet rs = stmt.getGeneratedKeys()) {
        if (rs.next()) {
          value.setId(rs.getString(1));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return value;
  }

  @Override
  public StockMovement updateStockMovement(StockMovement value) {
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
      sql.append("movement_type = CAST(? AS movement_type), ");
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
      return value;
    }

    sql.setLength(sql.length() - 2);
    sql.append(" WHERE id = ?");
    parameters.add(UUID.fromString(value.getId()));

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
      for (int index = 0; index < parameters.size(); index++) {
        stmt.setObject(index + 1, parameters.get(index));
      }
      stmt.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return value;
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
      e.printStackTrace();
    }
  }
}
