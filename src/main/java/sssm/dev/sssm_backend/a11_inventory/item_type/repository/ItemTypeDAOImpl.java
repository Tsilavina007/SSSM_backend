package sssm.dev.sssm_backend.a11_inventory.item_type.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a01_common.enums.ItemStatus;
import sssm.dev.sssm_backend.a11_inventory.item_type.entity.ItemType;

public class ItemTypeDAOImpl implements ItemTypeDAO {

  @Override
  public List<ItemType> findAll(int page, int pageSize) {
    String sql = "SELECT * FROM security.item_type ORDER BY id LIMIT ? OFFSET ?";
    List<ItemType> values = new ArrayList<>();
    int offset = (page - 1) * pageSize;

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, pageSize);
      stmt.setInt(2, offset);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          values.add(
              new ItemType(
                  rs.getString("id"),
                  rs.getString("company_id"),
                  rs.getString("category_id"),
                  rs.getString("code"),
                  rs.getString("name"),
                  rs.getString("description"),
                  rs.getString("unit"),
                  rs.getBigDecimal("quantity_total"),
                  rs.getBigDecimal("minimum_stock"),
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
  public List<ItemType> search(String value, int page, int pageSize) {
    String sql =
        "SELECT * FROM security.item_type WHERE code ILIKE ? OR name ILIKE ? OR description ILIKE ? ORDER BY id LIMIT ? OFFSET ?";
    List<ItemType> values = new ArrayList<>();
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
              new ItemType(
                  rs.getString("id"),
                  rs.getString("company_id"),
                  rs.getString("category_id"),
                  rs.getString("code"),
                  rs.getString("name"),
                  rs.getString("description"),
                  rs.getString("unit"),
                  rs.getBigDecimal("quantity_total"),
                  rs.getBigDecimal("minimum_stock"),
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
  public Optional<ItemType> findById(String id) {
    String sql = "SELECT * FROM security.item_type WHERE id = ?";
    UUID uuid = UUID.fromString(id);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, uuid);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(
              new ItemType(
                  rs.getString("id"),
                  rs.getString("company_id"),
                  rs.getString("category_id"),
                  rs.getString("code"),
                  rs.getString("name"),
                  rs.getString("description"),
                  rs.getString("unit"),
                  rs.getBigDecimal("quantity_total"),
                  rs.getBigDecimal("minimum_stock"),
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
  public ItemType create(ItemType value) {
    String sql =
        "INSERT INTO security.item_type (company_id, category_id, code, name, description, unit, quantity_total, minimum_stock, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS item_status))";

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      stmt.setObject(
          1, value.getCompany_id() == null ? null : UUID.fromString(value.getCompany_id()));
      stmt.setObject(
          2, value.getCategory_id() == null ? null : UUID.fromString(value.getCategory_id()));
      stmt.setString(3, value.getCode());
      stmt.setString(4, value.getName());
      stmt.setString(5, value.getDescription());
      stmt.setString(6, value.getUnit());
      stmt.setBigDecimal(7, value.getQuantity_total());
      stmt.setBigDecimal(8, value.getMinimum_stock());
      stmt.setString(
          9, value.getStatus() == null ? ItemStatus.AVAILABLE.name() : value.getStatus().name());
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
  public ItemType update(ItemType value) {
    StringBuilder sql = new StringBuilder("UPDATE security.item_type SET ");
    List<Object> parameters = new ArrayList<>();

    if (value.getCompany_id() != null) {
      sql.append("company_id = ?, ");
      parameters.add(UUID.fromString(value.getCompany_id()));
    }
    if (value.getCategory_id() != null) {
      sql.append("category_id = ?, ");
      parameters.add(UUID.fromString(value.getCategory_id()));
    }
    if (value.getCode() != null) {
      sql.append("code = ?, ");
      parameters.add(value.getCode());
    }
    if (value.getName() != null) {
      sql.append("name = ?, ");
      parameters.add(value.getName());
    }
    if (value.getDescription() != null) {
      sql.append("description = ?, ");
      parameters.add(value.getDescription());
    }
    if (value.getUnit() != null) {
      sql.append("unit = ?, ");
      parameters.add(value.getUnit());
    }
    if (value.getQuantity_total() != null) {
      sql.append("quantity_total = ?, ");
      parameters.add(value.getQuantity_total());
    }
    if (value.getMinimum_stock() != null) {
      sql.append("minimum_stock = ?, ");
      parameters.add(value.getMinimum_stock());
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
    String sql = "DELETE FROM security.item_type WHERE id = ?";
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
