package sssm.dev.sssm_backend.a11_inventory.item_assignment.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
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

  private static final String SELECT_COLUMNS =
      "id, company_id, item_id, employee_id, post_id, assigned_at, returned_at, "
          + "condition_on_assignment, condition_on_return, notes, created_at, updated_at, "
          + "assigned_by, returned_by, expected_return_at, source_warehouse_id, "
          + "voided_at, voided_by, void_reason";

  @Override
  public List<ItemAssignmentResponse> getAllItemAssignments(
      int page, int pageSize, boolean activeOnly) {
    return list(
        "SELECT " + SELECT_COLUMNS + " FROM security.item_assignment WHERE voided_at IS NULL"
            + (activeOnly ? " AND returned_at IS NULL" : "")
            + " ORDER BY assigned_at DESC, id LIMIT ? OFFSET ?",
        page,
        pageSize);
  }

  @Override
  public List<ItemAssignmentResponse> searchItemAssignmentsByName(
      String value, int page, int pageSize, boolean activeOnly) {
    return list(
        "SELECT "
            + SELECT_COLUMNS
            + " FROM security.item_assignment WHERE voided_at IS NULL"
            + (activeOnly ? " AND returned_at IS NULL" : "")
            + " AND (condition_on_assignment ILIKE ? OR condition_on_return ILIKE ? OR notes ILIKE ?)"
            + " ORDER BY assigned_at DESC, id LIMIT ? OFFSET ?",
        page,
        pageSize,
        "%" + value + "%",
        "%" + value + "%",
        "%" + value + "%");
  }

  @Override
  public List<ItemAssignmentResponse> getItemAssignmentsByItem(
      String itemId, int page, int pageSize) {
    return list(
        "SELECT " + SELECT_COLUMNS
            + " FROM security.item_assignment WHERE item_id = ? AND voided_at IS NULL"
            + " ORDER BY assigned_at DESC, id LIMIT ? OFFSET ?",
        page,
        pageSize,
        UUID.fromString(itemId));
  }

  @Override
  public List<ItemAssignmentResponse> getItemAssignmentsByEmployee(
      String employeeId, int page, int pageSize) {
    return list(
        "SELECT " + SELECT_COLUMNS
            + " FROM security.item_assignment WHERE employee_id = ? AND voided_at IS NULL"
            + " ORDER BY assigned_at DESC, id LIMIT ? OFFSET ?",
        page,
        pageSize,
        UUID.fromString(employeeId));
  }

  @Override
  public List<ItemAssignmentResponse> getItemAssignmentsByPost(
      String postId, int page, int pageSize) {
    return list(
        "SELECT " + SELECT_COLUMNS
            + " FROM security.item_assignment WHERE post_id = ? AND voided_at IS NULL"
            + " ORDER BY assigned_at DESC, id LIMIT ? OFFSET ?",
        page,
        pageSize,
        UUID.fromString(postId));
  }

  @Override
  public Optional<ItemAssignmentResponse> getItemAssignmentById(String id) {
    return one(
        "SELECT " + SELECT_COLUMNS + " FROM security.item_assignment WHERE id = ?",
        UUID.fromString(id));
  }

  @Override
  public Optional<ItemAssignmentResponse> getActiveItemAssignmentByItem(String itemId) {
    return one(
        "SELECT " + SELECT_COLUMNS
            + " FROM security.item_assignment WHERE item_id = ?"
            + " AND returned_at IS NULL AND voided_at IS NULL"
            + " ORDER BY assigned_at DESC LIMIT 1",
        UUID.fromString(itemId));
  }

  @Override
  public ItemAssignmentResponse createItemAssignment(
      ItemAssignmentCreateRequest value,
      String companyId,
      String sourceWarehouseId,
      String assignedBy) {
    StringBuilder columns =
        new StringBuilder(
            "company_id, item_id, employee_id, post_id, source_warehouse_id, assigned_by");
    StringBuilder placeholders = new StringBuilder("?, ?, ?, ?, ?, ?");
    List<Object> parameters = new ArrayList<>();

    parameters.add(companyId == null ? null : UUID.fromString(companyId));
    parameters.add(UUID.fromString(value.getItem_id()));
    parameters.add(UUID.fromString(value.getEmployee_id()));
    parameters.add(UUID.fromString(value.getPost_id()));
    parameters.add(sourceWarehouseId == null ? null : UUID.fromString(sourceWarehouseId));
    parameters.add(assignedBy == null ? null : UUID.fromString(assignedBy));

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
    if (value.getExpected_return_at() != null) {
      columns.append(", expected_return_at");
      placeholders.append(", ?");
      parameters.add(value.getExpected_return_at());
    }

    String sql =
        "INSERT INTO security.item_assignment ("
            + columns
            + ") VALUES ("
            + placeholders
            + ") RETURNING id";

    String generatedId = null;
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
      throw new ServerException("La base n'a pas renvoyé l'identifiant de l'affectation créée.");
    }
    return getItemAssignmentById(generatedId)
        .orElseThrow(
            () ->
                new ServerException(
                    "L'affectation créée n'a pas pu être relue depuis la base."));
  }

  @Override
  public ItemAssignmentResponse updateItemAssignmentMetadata(
      String id, ItemAssignmentUpdateRequest value) {
    StringBuilder sql =
        new StringBuilder(
            "UPDATE security.item_assignment SET updated_at = now()");
    List<Object> parameters = new ArrayList<>();

    if (value.getCondition_on_return() != null) {
      sql.append(", condition_on_return = ?");
      parameters.add(value.getCondition_on_return());
    }
    if (value.getNotes() != null) {
      sql.append(", notes = ?");
      parameters.add(value.getNotes());
    }
    if (value.getExpected_return_at() != null) {
      sql.append(", expected_return_at = ?");
      parameters.add(value.getExpected_return_at());
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

    sql.append(" WHERE id = ? AND voided_at IS NULL");
    parameters.add(UUID.fromString(id));

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
      for (int index = 0; index < parameters.size(); index++) {
        stmt.setObject(index + 1, parameters.get(index));
      }
      int updated = stmt.executeUpdate();
      if (updated == 0) {
        throw new NotFoundException(
            "Affectation d'article introuvable",
            "Aucune affectation d'article active ne correspond à l'identifiant " + id + ".",
            "NOT_FOUND");
      }
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
  public int markReturned(
      String id, Timestamp returnedAt, String returnedBy, String conditionOnReturn) {
    String sql =
        "UPDATE security.item_assignment SET returned_at = ?, returned_by = ?, "
            + "condition_on_return = COALESCE(?, condition_on_return), updated_at = now() "
            + "WHERE id = ? AND returned_at IS NULL AND voided_at IS NULL";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setTimestamp(1, returnedAt);
      stmt.setObject(2, returnedBy == null ? null : UUID.fromString(returnedBy));
      stmt.setString(3, conditionOnReturn);
      stmt.setObject(4, UUID.fromString(id));
      return stmt.executeUpdate();
    } catch (SQLException e) {
      throw new ServerException(e);
    }
  }

  @Override
  public int voidActiveItemAssignment(
      String id, Timestamp voidedAt, String voidedBy, String reason) {
    String sql =
        "UPDATE security.item_assignment SET returned_at = ?, returned_by = ?, "
            + "voided_at = ?, voided_by = ?, void_reason = ?, updated_at = now() "
            + "WHERE id = ? AND returned_at IS NULL AND voided_at IS NULL";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setTimestamp(1, voidedAt);
      stmt.setObject(2, voidedBy == null ? null : UUID.fromString(voidedBy));
      stmt.setTimestamp(3, voidedAt);
      stmt.setObject(4, voidedBy == null ? null : UUID.fromString(voidedBy));
      stmt.setString(5, reason);
      stmt.setObject(6, UUID.fromString(id));
      return stmt.executeUpdate();
    } catch (SQLException e) {
      throw new ServerException(e);
    }
  }

  @Override
  public int archiveInactiveItemAssignment(
      String id, Timestamp voidedAt, String voidedBy, String reason) {
    String sql =
        "UPDATE security.item_assignment SET voided_at = ?, voided_by = ?, "
            + "void_reason = ?, updated_at = now() "
            + "WHERE id = ? AND returned_at IS NOT NULL AND voided_at IS NULL";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setTimestamp(1, voidedAt);
      stmt.setObject(2, voidedBy == null ? null : UUID.fromString(voidedBy));
      stmt.setString(3, reason);
      stmt.setObject(4, UUID.fromString(id));
      return stmt.executeUpdate();
    } catch (SQLException e) {
      throw new ServerException(e);
    }
  }

  @Override
  public void restoreItemAssignment(String id) {
    String sql =
        "UPDATE security.item_assignment SET voided_at = NULL, voided_by = NULL, "
            + "void_reason = NULL, updated_at = now() WHERE id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, UUID.fromString(id));
      stmt.executeUpdate();
    } catch (SQLException e) {
      throw new ServerException(e);
    }
  }

  private List<ItemAssignmentResponse> list(String sql, int page, int pageSize, Object... args) {
    int safePage = Math.max(1, page);
    int safeSize = Math.min(500, Math.max(1, pageSize));
    List<ItemAssignmentResponse> values = new ArrayList<>();
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      int index = 1;
      for (Object arg : args) {
        stmt.setObject(index++, arg);
      }
      stmt.setInt(index++, safeSize);
      stmt.setInt(index, (safePage - 1) * safeSize);
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

  private Optional<ItemAssignmentResponse> one(String sql, Object... args) {
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      for (int index = 0; index < args.length; index++) {
        stmt.setObject(index + 1, args[index]);
      }
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

  private ItemAssignmentResponse mapRowToItemAssignmentResponse(ResultSet rs)
      throws SQLException {
    return new ItemAssignmentResponse(
        rs.getString("id"),
        rs.getString("company_id"),
        rs.getString("item_id"),
        rs.getString("employee_id"),
        rs.getString("post_id"),
        rs.getTimestamp("assigned_at"),
        rs.getTimestamp("returned_at"),
        rs.getString("condition_on_assignment"),
        rs.getString("condition_on_return"),
        rs.getString("notes"),
        rs.getTimestamp("created_at"),
        rs.getTimestamp("updated_at"),
        rs.getString("assigned_by"),
        rs.getString("returned_by"),
        rs.getTimestamp("expected_return_at"),
        rs.getString("source_warehouse_id"),
        rs.getTimestamp("voided_at"),
        rs.getString("voided_by"),
        rs.getString("void_reason"));
  }
}
