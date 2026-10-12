package sssm.dev.sssm_backend.a11_inventory.item_assignment.repository;

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
import sssm.dev.sssm_backend.a11_inventory.item_assignment.entity.ItemAssignment;

public class ItemAssignmentDAOImpl implements ItemAssignmentDAO {

  @Override
  public List<ItemAssignment> getAllItemAssignments(int page, int pageSize) {
    String sql = "SELECT * FROM security.item_assignment ORDER BY id LIMIT ? OFFSET ?";
    List<ItemAssignment> values = new ArrayList<>();
    int offset = (page - 1) * pageSize;

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, pageSize);
      stmt.setInt(2, offset);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          values.add(
              new ItemAssignment(
                  rs.getString("id"),
                  rs.getString("item_id"),
                  rs.getString("employee_id"),
                  rs.getString("post_id"),
                  rs.getTimestamp("assigned_at"),
                  rs.getTimestamp("returned_at"),
                  rs.getString("condition_on_assignment"),
                  rs.getString("condition_on_return"),
                  rs.getString("notes")));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return values;
  }

  @Override
  public List<ItemAssignment> searchItemAssignmentsByName(String value, int page, int pageSize) {
    String sql =
        "SELECT * FROM security.item_assignment WHERE condition_on_assignment ILIKE ? OR condition_on_return ILIKE ? OR notes ILIKE ? ORDER BY id LIMIT ? OFFSET ?";
    List<ItemAssignment> values = new ArrayList<>();
    int offset = (page - 1) * pageSize;

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      String searchPattern = "%" + value + "%";
      stmt.setString(1, searchPattern);
      stmt.setString(2, searchPattern);
      stmt.setString(3, searchPattern);
      stmt.setInt(4, pageSize);
      stmt.setInt(5, offset);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          values.add(
              new ItemAssignment(
                  rs.getString("id"),
                  rs.getString("item_id"),
                  rs.getString("employee_id"),
                  rs.getString("post_id"),
                  rs.getTimestamp("assigned_at"),
                  rs.getTimestamp("returned_at"),
                  rs.getString("condition_on_assignment"),
                  rs.getString("condition_on_return"),
                  rs.getString("notes")));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return values;
  }

  @Override
  public Optional<ItemAssignment> getItemAssignmentById(String id) {
    String sql = "SELECT * FROM security.item_assignment WHERE id = ?";
    UUID uuid = UUID.fromString(id);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, uuid);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(
              new ItemAssignment(
                  rs.getString("id"),
                  rs.getString("item_id"),
                  rs.getString("employee_id"),
                  rs.getString("post_id"),
                  rs.getTimestamp("assigned_at"),
                  rs.getTimestamp("returned_at"),
                  rs.getString("condition_on_assignment"),
                  rs.getString("condition_on_return"),
                  rs.getString("notes")));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }

  @Override
  public ItemAssignment createItemAssignment(ItemAssignment value) {
    String sql =
        "INSERT INTO security.item_assignment (item_id, employee_id, post_id, assigned_at, returned_at, condition_on_assignment, condition_on_return, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      stmt.setObject(1, value.getItem_id() == null ? null : UUID.fromString(value.getItem_id()));
      stmt.setObject(
          2, value.getEmployee_id() == null ? null : UUID.fromString(value.getEmployee_id()));
      stmt.setObject(3, value.getPost_id() == null ? null : UUID.fromString(value.getPost_id()));
      stmt.setTimestamp(4, value.getAssigned_at());
      stmt.setTimestamp(5, value.getReturned_at());
      stmt.setString(6, value.getCondition_on_assignment());
      stmt.setString(7, value.getCondition_on_return());
      stmt.setString(8, value.getNotes());
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
  public ItemAssignment updateItemAssignment(ItemAssignment value) {
    StringBuilder sql = new StringBuilder("UPDATE security.item_assignment SET ");
    List<Object> parameters = new ArrayList<>();

    if (value.getItem_id() != null) {
      sql.append("item_id = ?, ");
      parameters.add(UUID.fromString(value.getItem_id()));
    }
    if (value.getEmployee_id() != null) {
      sql.append("employee_id = ?, ");
      parameters.add(UUID.fromString(value.getEmployee_id()));
    }
    if (value.getPost_id() != null) {
      sql.append("post_id = ?, ");
      parameters.add(UUID.fromString(value.getPost_id()));
    }
    if (value.getAssigned_at() != null) {
      sql.append("assigned_at = ?, ");
      parameters.add(value.getAssigned_at());
    }
    if (value.getReturned_at() != null) {
      sql.append("returned_at = ?, ");
      parameters.add(value.getReturned_at());
    }
    if (value.getCondition_on_assignment() != null) {
      sql.append("condition_on_assignment = ?, ");
      parameters.add(value.getCondition_on_assignment());
    }
    if (value.getCondition_on_return() != null) {
      sql.append("condition_on_return = ?, ");
      parameters.add(value.getCondition_on_return());
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
  public void deleteItemAssignment(String id) {
    String sql = "DELETE FROM security.item_assignment WHERE id = ?";
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
