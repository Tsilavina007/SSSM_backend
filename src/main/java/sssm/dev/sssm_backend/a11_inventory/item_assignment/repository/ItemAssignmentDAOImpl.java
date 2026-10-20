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
import org.springframework.stereotype.Repository;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.dto.ItemAssignmentCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.dto.ItemAssignmentResponse;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.dto.ItemAssignmentUpdateRequest;

@Repository
public class ItemAssignmentDAOImpl implements ItemAssignmentDAO {

  @Override
  public List<ItemAssignmentResponse> getAllItemAssignments(int page, int pageSize) {
    String sql = "SELECT * FROM security.item_assignment ORDER BY id LIMIT ? OFFSET ?";
    List<ItemAssignmentResponse> values = new ArrayList<>();
    int offset = (page - 1) * pageSize;

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, pageSize);
      stmt.setInt(2, offset);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          values.add(mapRowToItemAssignmentResponse(rs));
        }
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return values;
  }

  @Override
  public List<ItemAssignmentResponse> searchItemAssignmentsByName(
      String value, int page, int pageSize) {
    String sql =
        "SELECT * FROM security.item_assignment WHERE condition_on_assignment ILIKE ? OR condition_on_return ILIKE ? OR notes ILIKE ? ORDER BY id LIMIT ? OFFSET ?";
    List<ItemAssignmentResponse> values = new ArrayList<>();
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
          values.add(mapRowToItemAssignmentResponse(rs));
        }
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return values;
  }

  @Override
  public Optional<ItemAssignmentResponse> getItemAssignmentById(String id) {
    String sql = "SELECT * FROM security.item_assignment WHERE id = ?";
    UUID uuid = UUID.fromString(id);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, uuid);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapRowToItemAssignmentResponse(rs));
        }
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return Optional.empty();
  }

  @Override
  public ItemAssignmentResponse createItemAssignment(ItemAssignmentCreateRequest value) {
    // Colonnes requises toujours écrites ; les autres sont omises quand
    // absentes pour laisser s'appliquer les défauts PostgreSQL
    // (assigned_at NOT NULL DEFAULT now()).
    StringBuilder columns = new StringBuilder("item_id, employee_id, post_id");
    StringBuilder placeholders = new StringBuilder("?, ?, ?");
    List<Object> parameters = new ArrayList<>();
    String generatedId = null;

    parameters.add(value.getItem_id() == null ? null : UUID.fromString(value.getItem_id()));
    parameters.add(value.getEmployee_id() == null ? null : UUID.fromString(value.getEmployee_id()));
    parameters.add(value.getPost_id() == null ? null : UUID.fromString(value.getPost_id()));

    if (value.getAssigned_at() != null) {
      columns.append(", assigned_at");
      placeholders.append(", ?");
      parameters.add(value.getAssigned_at());
    }
    if (value.getReturned_at() != null) {
      columns.append(", returned_at");
      placeholders.append(", ?");
      parameters.add(value.getReturned_at());
    }
    if (value.getCondition_on_assignment() != null) {
      columns.append(", condition_on_assignment");
      placeholders.append(", ?");
      parameters.add(value.getCondition_on_assignment());
    }
    if (value.getCondition_on_return() != null) {
      columns.append(", condition_on_return");
      placeholders.append(", ?");
      parameters.add(value.getCondition_on_return());
    }
    if (value.getNotes() != null) {
      columns.append(", notes");
      placeholders.append(", ?");
      parameters.add(value.getNotes());
    }

    String sql =
        "INSERT INTO security.item_assignment (" + columns + ") VALUES (" + placeholders + ")";

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      for (int index = 0; index < parameters.size(); index++) {
        stmt.setObject(index + 1, parameters.get(index));
      }
      stmt.executeUpdate();

      try (ResultSet rs = stmt.getGeneratedKeys()) {
        if (rs.next()) {
          generatedId = rs.getString(1);
        }
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return new ItemAssignmentResponse(
        generatedId,
        value.getItem_id(),
        value.getEmployee_id(),
        value.getPost_id(),
        value.getAssigned_at(),
        value.getReturned_at(),
        value.getCondition_on_assignment(),
        value.getCondition_on_return(),
        value.getNotes());
  }

  @Override
  public ItemAssignmentResponse updateItemAssignment(String id, ItemAssignmentUpdateRequest value) {
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
      return getItemAssignmentById(id)
          .orElseThrow(
              () ->
                  new NotFoundException(
                      "Affectation d'article introuvable",
                      "Aucune affectation d'article ne correspond à l'identifiant " + id + ".",
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
    return getItemAssignmentById(id)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Affectation d'article introuvable",
                    "Aucune affectation d'article ne correspond à l'identifiant " + id + ".",
                    "NOT_FOUND"));
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
      throw new ServerException(e);
    }
  }

  private ItemAssignmentResponse mapRowToItemAssignmentResponse(ResultSet rs) throws SQLException {
    return new ItemAssignmentResponse(
        rs.getString("id"),
        rs.getString("item_id"),
        rs.getString("employee_id"),
        rs.getString("post_id"),
        rs.getTimestamp("assigned_at"),
        rs.getTimestamp("returned_at"),
        rs.getString("condition_on_assignment"),
        rs.getString("condition_on_return"),
        rs.getString("notes"));
  }
}
