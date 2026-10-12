package sssm.dev.sssm_backend.a03_authentication.permission.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a03_authentication.permission.entity.Permission;

public class PermissionDAOImpl implements PermissionDAO {

  @Override
  public List<Permission> findAll(int page, int pageSize) {
    String sql = "SELECT * FROM security.permission LIMIT ? OFFSET ?";
    List<Permission> permissions = new ArrayList<>();
    int offset = (page - 1) * pageSize;

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, pageSize);
      stmt.setInt(2, offset);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          Permission permission = mapRowToPermission(rs);
          permissions.add(permission);
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return permissions;
  }

  @Override
  public Optional<Permission> findById(String permissionId) {
    String sql = "SELECT * FROM security.permission WHERE id = ?";
    UUID id = UUID.fromString(permissionId);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, id);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapRowToPermission(rs));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }

  @Override
  public Permission create(Permission permission) {
    String sql = "INSERT INTO security.permission (code, name, description) VALUES (?, ?, ?)";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      stmt.setString(1, permission.getCode());
      stmt.setString(2, permission.getName());
      stmt.setString(3, permission.getDescription());
      stmt.executeUpdate();

      try (ResultSet rs = stmt.getGeneratedKeys()) {
        if (rs.next()) {
          permission.setId(rs.getString(1));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return permission;
  }

  @Override
  public Permission update(Permission permission) {
    StringBuilder sql = new StringBuilder("UPDATE security.permission SET ");
    List<Object> parameters = new ArrayList<>();

    if (permission.getCode() != null) {
      sql.append("code = ?, ");
      parameters.add(permission.getCode());
    }
    if (permission.getName() != null) {
      sql.append("name = ?, ");
      parameters.add(permission.getName());
    }
    if (permission.getDescription() != null) {
      sql.append("description = ?, ");
      parameters.add(permission.getDescription());
    }

    if (parameters.isEmpty()) {
      return permission;
    }

    sql.setLength(sql.length() - 2);
    sql.append(" WHERE id = ?");
    parameters.add(UUID.fromString(permission.getId()));

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
      for (int index = 0; index < parameters.size(); index++) {
        stmt.setObject(index + 1, parameters.get(index));
      }
      stmt.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return permission;
  }

  @Override
  public void delete(String permissionId) {
    String sql = "DELETE FROM security.permission WHERE id = ?";
    UUID id = UUID.fromString(permissionId);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, id);
      stmt.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private Permission mapRowToPermission(ResultSet rs) throws SQLException {
    return new Permission(
        rs.getString("id"),
        rs.getString("code"),
        rs.getString("name"),
        rs.getString("description"));
  }
}
