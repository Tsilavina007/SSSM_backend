package sssm.dev.sssm_backend.a03_authentication.role.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a03_authentication.role.entity.Role;

public class RoleDAOImpl implements RoleDAO {

  @Override
  public List<Role> findAll(int page, int pageSize) {
    String sql = "SELECT * FROM security.role LIMIT ? OFFSET ?";
    List<Role> roles = new ArrayList<>();
    int offset = (page - 1) * pageSize;

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, pageSize);
      stmt.setInt(2, offset);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          Role role = mapRowToRole(rs);
          roles.add(role);
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return roles;
  }

  @Override
  public Optional<Role> findById(String roleId) {
    String sql = "SELECT * FROM security.role WHERE id = ?";
    UUID id = UUID.fromString(roleId);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, id);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapRowToRole(rs));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }

  @Override
  public Optional<Role> findGlobalByName(String name) {
    String sql = "SELECT * FROM security.role WHERE company_id IS NULL AND lower(name) = lower(?)";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, name);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) return Optional.of(mapRowToRole(rs));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }

  @Override
  public Role create(Role role) {
    String sql = "INSERT INTO security.role (company_id, name, description) VALUES (?, ?, ?)";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      stmt.setObject(1, UUID.fromString(role.getCompany_id()));
      stmt.setString(2, role.getName());
      stmt.setString(3, role.getDescription());
      stmt.executeUpdate();

      try (ResultSet rs = stmt.getGeneratedKeys()) {
        if (rs.next()) {
          role.setId(rs.getString(1));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return role;
  }

  @Override
  public Role update(Role role) {
    StringBuilder sql = new StringBuilder("UPDATE security.role SET ");
    List<Object> parameters = new ArrayList<>();

    if (role.getCompany_id() != null) {
      sql.append("company_id = ?, ");
      parameters.add(UUID.fromString(role.getCompany_id()));
    }
    if (role.getName() != null) {
      sql.append("name = ?, ");
      parameters.add(role.getName());
    }
    if (role.getDescription() != null) {
      sql.append("description = ?, ");
      parameters.add(role.getDescription());
    }

    if (parameters.isEmpty()) {
      return role;
    }

    sql.setLength(sql.length() - 2);
    sql.append(" WHERE id = ?");
    parameters.add(UUID.fromString(role.getId()));

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
      for (int index = 0; index < parameters.size(); index++) {
        stmt.setObject(index + 1, parameters.get(index));
      }
      stmt.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return role;
  }

  @Override
  public void delete(String roleId) {
    String sql = "DELETE FROM security.role WHERE id = ?";
    UUID id = UUID.fromString(roleId);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, id);
      stmt.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void assignPermission(String roleId, String permissionId) {
    String sql =
        "INSERT INTO security.role_permission (role_id, permission_id) VALUES (?, ?) "
            + "ON CONFLICT DO NOTHING";
    executePair(sql, roleId, permissionId);
  }

  @Override
  public void removePermission(String roleId, String permissionId) {
    String sql = "DELETE FROM security.role_permission WHERE role_id = ? AND permission_id = ?";
    executePair(sql, roleId, permissionId);
  }

  private void executePair(String sql, String firstId, String secondId) {
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, UUID.fromString(firstId));
      stmt.setObject(2, UUID.fromString(secondId));
      stmt.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private Role mapRowToRole(ResultSet rs) throws SQLException {
    return new Role(
        rs.getString("id"),
        rs.getString("company_id"),
        rs.getString("name"),
        rs.getString("description"));
  }
}
