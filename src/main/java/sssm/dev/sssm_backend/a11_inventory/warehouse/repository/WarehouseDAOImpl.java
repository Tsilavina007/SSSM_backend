package sssm.dev.sssm_backend.a11_inventory.warehouse.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a11_inventory.warehouse.entity.Warehouse;

public class WarehouseDAOImpl implements WarehouseDAO {

  @Override
  public List<Warehouse> findAll(int page, int pageSize) {
    String sql = "SELECT * FROM security.warehouse ORDER BY id LIMIT ? OFFSET ?";
    List<Warehouse> values = new ArrayList<>();
    int offset = (page - 1) * pageSize;

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, pageSize);
      stmt.setInt(2, offset);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          values.add(
              new Warehouse(
                  rs.getString("id"),
                  rs.getString("company_id"),
                  rs.getString("branch_id"),
                  rs.getString("name"),
                  rs.getString("address"),
                  rs.getBoolean("active")));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return values;
  }

  @Override
  public List<Warehouse> search(String value, int page, int pageSize) {
    String sql =
        "SELECT * FROM security.warehouse WHERE name ILIKE ? OR address ILIKE ? ORDER BY id LIMIT ? OFFSET ?";
    List<Warehouse> values = new ArrayList<>();
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
              new Warehouse(
                  rs.getString("id"),
                  rs.getString("company_id"),
                  rs.getString("branch_id"),
                  rs.getString("name"),
                  rs.getString("address"),
                  rs.getBoolean("active")));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return values;
  }

  @Override
  public Optional<Warehouse> findById(String id) {
    String sql = "SELECT * FROM security.warehouse WHERE id = ?";
    UUID uuid = UUID.fromString(id);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, uuid);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(
              new Warehouse(
                  rs.getString("id"),
                  rs.getString("company_id"),
                  rs.getString("branch_id"),
                  rs.getString("name"),
                  rs.getString("address"),
                  rs.getBoolean("active")));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }

  @Override
  public Warehouse create(Warehouse value) {
    String sql =
        "INSERT INTO security.warehouse (company_id, branch_id, name, address, active) VALUES (?, ?, ?, ?, ?)";

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      stmt.setObject(
          1, value.getCompany_id() == null ? null : UUID.fromString(value.getCompany_id()));
      stmt.setObject(
          2, value.getBranch_id() == null ? null : UUID.fromString(value.getBranch_id()));
      stmt.setString(3, value.getName());
      stmt.setString(4, value.getAddress());
      stmt.setBoolean(5, value.getActive() == null ? true : value.getActive());
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
  public Warehouse update(Warehouse value) {
    StringBuilder sql = new StringBuilder("UPDATE security.warehouse SET ");
    List<Object> parameters = new ArrayList<>();

    if (value.getCompany_id() != null) {
      sql.append("company_id = ?, ");
      parameters.add(UUID.fromString(value.getCompany_id()));
    }
    if (value.getBranch_id() != null) {
      sql.append("branch_id = ?, ");
      parameters.add(UUID.fromString(value.getBranch_id()));
    }
    if (value.getName() != null) {
      sql.append("name = ?, ");
      parameters.add(value.getName());
    }
    if (value.getAddress() != null) {
      sql.append("address = ?, ");
      parameters.add(value.getAddress());
    }
    if (value.getActive() != null) {
      sql.append("active = ?, ");
      parameters.add(value.getActive());
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
    String sql = "DELETE FROM security.warehouse WHERE id = ?";
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
