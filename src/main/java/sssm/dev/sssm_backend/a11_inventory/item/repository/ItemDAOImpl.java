package sssm.dev.sssm_backend.a11_inventory.item.repository;

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
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;
import sssm.dev.sssm_backend.a01_common.enums.ItemStatus;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemResponse;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemUpdateRequest;

@Repository 
public class ItemDAOImpl implements ItemDAO {

  @Override
  public List<ItemResponse> getAllItems(int page, int pageSize) {
    String sql = "SELECT * FROM security.item ORDER BY id LIMIT ? OFFSET ?";
    List<ItemResponse> values = new ArrayList<>();
    int offset = (page - 1) * pageSize;

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, pageSize);
      stmt.setInt(2, offset);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          values.add(mapRowToItemResponse(rs));
        }
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return values;
  }

  @Override
  public List<ItemResponse> searchItemsByName(String value, int page, int pageSize) {
    String sql =
        "SELECT * FROM security.item WHERE qr_code ILIKE ? OR description ILIKE ? ORDER BY id LIMIT ? OFFSET ?";
    List<ItemResponse> values = new ArrayList<>();
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
          values.add(mapRowToItemResponse(rs));
        }
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return values;
  }

  @Override
  public Optional<ItemResponse> getItemById(String id) {
    String sql = "SELECT * FROM security.item WHERE id = ?";
    UUID uuid = UUID.fromString(id);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, uuid);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapRowToItemResponse(rs));
        }
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return Optional.empty();
  }

  @Override
  public ItemResponse createItem(ItemCreateRequest value) {
    String sql =
        "INSERT INTO security.item (company_id, category_id, item_type_id, qr_code, description, status, current_warehouse_id) VALUES (?, ?, ?, ?, ?, CAST(? AS security.item_status), ?)";

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      stmt.setObject(
          1, value.getCompany_id() == null ? null : UUID.fromString(value.getCompany_id()));
      stmt.setObject(
          2, value.getCategory_id() == null ? null : UUID.fromString(value.getCategory_id()));
      stmt.setObject(
          3, value.getItem_type_id() == null ? null : UUID.fromString(value.getItem_type_id()));
      stmt.setString(4, value.getQr_code());
      stmt.setString(5, value.getDescription());
      stmt.setString(
          6, value.getStatus() == null ? ItemStatus.AVAILABLE.name() : value.getStatus().name());
      stmt.setObject(
          7, value.getCurrent_warehouse_id() == null
              ? null
              : UUID.fromString(value.getCurrent_warehouse_id()));
      stmt.executeUpdate();

      try (ResultSet rs = stmt.getGeneratedKeys()) {
        if (rs.next()) {
          return mapRowToItemResponse(rs);
        }
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return null;
  }

  @Override
  public ItemResponse updateItem(String id, ItemUpdateRequest value) {
    StringBuilder sql = new StringBuilder("UPDATE security.item SET ");
    List<Object> parameters = new ArrayList<>();

    if (value.getCompany_id() != null) {
      sql.append("company_id = ?, ");
      parameters.add(UUID.fromString(value.getCompany_id()));
    }
    if (value.getCategory_id() != null) {
      sql.append("category_id = ?, ");
      parameters.add(UUID.fromString(value.getCategory_id()));
    }
    if (value.getItem_type_id() != null) {
      sql.append("item_type_id = ?, ");
      parameters.add(UUID.fromString(value.getItem_type_id()));
    }
    if (value.getQr_code() != null) {
      sql.append("qr_code = ?, ");
      parameters.add(value.getQr_code());
    }
    if (value.getDescription() != null) {
      sql.append("description = ?, ");
      parameters.add(value.getDescription());
    }
    if (value.getStatus() != null) {
      sql.append("status = CAST(? AS security.item_status), ");
      parameters.add(value.getStatus().name());
    }
    if (value.getCurrent_warehouse_id() != null) {
      sql.append("current_warehouse_id = ?, ");
      parameters.add(UUID.fromString(value.getCurrent_warehouse_id()));
    }
    if (parameters.isEmpty()) {
      return getItemById(id).get();
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
    return getItemById(id).get();
  }

  @Override
  public void deleteItem(String id) {
    String sql = "DELETE FROM security.item WHERE id = ?";
    UUID uuid = UUID.fromString(id);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, uuid);
      stmt.executeUpdate();
    } catch (SQLException e) {
      throw new ServerException(e);
    }
  }

  public ItemResponse mapRowToItemResponse(ResultSet rs) throws SQLException {
    return new ItemResponse(
        rs.getString("id"),
        rs.getString("company_id"),
        rs.getString("category_id"),
        rs.getString("item_type_id"),
        rs.getString("qr_code"),
        rs.getString("description"),
        ItemStatus.valueOf(rs.getString("status")),
        rs.getString("current_warehouse_id"),
        rs.getTimestamp("created_at"),
        rs.getTimestamp("updated_at"),
        rs.getTimestamp("archived_at"),
        rs.getString("archived_by"),
        rs.getTimestamp("last_inventory_at")
      );
  }
}
