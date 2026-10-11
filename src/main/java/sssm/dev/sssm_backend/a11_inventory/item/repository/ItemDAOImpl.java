package sssm.dev.sssm_backend.a11_inventory.item.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a01_common.enums.ItemStatus;
import sssm.dev.sssm_backend.a11_inventory.item.entity.Item;

public class ItemDAOImpl implements ItemDAO {

  @Override
  public List<Item> findAll(int page, int pageSize) {
    String sql = "SELECT * FROM security.item ORDER BY id LIMIT ? OFFSET ?";
    List<Item> values = new ArrayList<>();
    int offset = (page - 1) * pageSize;

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, pageSize);
      stmt.setInt(2, offset);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          values.add(
              new Item(
                  rs.getString("id"),
                  rs.getString("company_id"),
                  rs.getString("category_id"),
                  rs.getString("item_type_id"),
                  rs.getString("qr_code"),
                  rs.getString("description"),
                  ItemStatus.valueOf(rs.getString("status")),
                  rs.getTimestamp("created_at"),
                  rs.getTimestamp("updated_at")));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return values;
  }

  @Override
  public List<Item> search(String value, int page, int pageSize) {
    String sql =
        "SELECT * FROM security.item WHERE qr_code ILIKE ? OR description ILIKE ? ORDER BY id LIMIT ? OFFSET ?";
    List<Item> values = new ArrayList<>();
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
              new Item(
                  rs.getString("id"),
                  rs.getString("company_id"),
                  rs.getString("category_id"),
                  rs.getString("item_type_id"),
                  rs.getString("qr_code"),
                  rs.getString("description"),
                  ItemStatus.valueOf(rs.getString("status")),
                  rs.getTimestamp("created_at"),
                  rs.getTimestamp("updated_at")));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return values;
  }

  @Override
  public Optional<Item> findById(String id) {
    String sql = "SELECT * FROM security.item WHERE id = ?";
    UUID uuid = UUID.fromString(id);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, uuid);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(
              new Item(
                  rs.getString("id"),
                  rs.getString("company_id"),
                  rs.getString("category_id"),
                  rs.getString("item_type_id"),
                  rs.getString("qr_code"),
                  rs.getString("description"),
                  ItemStatus.valueOf(rs.getString("status")),
                  rs.getTimestamp("created_at"),
                  rs.getTimestamp("updated_at")));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }

  @Override
  public Item create(Item value) {
    String sql =
        "INSERT INTO security.item (company_id, category_id, item_type_id, qr_code, description, status) VALUES (?, ?, ?, ?, ?, CAST(? AS item_status))";

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
  public Item update(Item value) {
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
      sql.append("status = CAST(? AS item_status), ");
      parameters.add(value.getStatus().name());
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
  public void delete(String id) {
    String sql = "DELETE FROM security.item WHERE id = ?";
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
