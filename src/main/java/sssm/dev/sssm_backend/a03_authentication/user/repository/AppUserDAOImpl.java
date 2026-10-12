package sssm.dev.sssm_backend.a03_authentication.user.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a01_common.enums.UserStatus;
import sssm.dev.sssm_backend.a03_authentication.user.entity.AppUser;

public class AppUserDAOImpl implements AppUserDAO {

  @Override
  public List<AppUser> findAll(int page, int pageSize) {
    String sql = "SELECT * FROM security.app_user LIMIT ? OFFSET ?";
    List<AppUser> users = new ArrayList<>();
    int offset = (page - 1) * pageSize;

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, pageSize);
      stmt.setInt(2, offset);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          AppUser user = mapRowToAppUser(rs);
          users.add(user);
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return users;
  }

  @Override
  public Optional<AppUser> findById(String userId) {
    String sql = "SELECT * FROM security.app_user WHERE id = ?";
    UUID id = UUID.fromString(userId);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, id);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapRowToAppUser(rs));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }

  @Override
  public Optional<AppUser> findByUsernameOrEmail(String login) {
    String sql = "SELECT * FROM security.app_user WHERE username = ? OR lower(email) = lower(?)";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, login);
      stmt.setString(2, login);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) return Optional.of(mapRowToAppUser(rs));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }

  @Override
  public List<String> findRoleNames(String userId) {
    String sql =
        "SELECT r.name FROM security.role r JOIN security.user_role ur ON ur.role_id = r.id "
            + "WHERE ur.user_id = ?";
    List<String> roles = new ArrayList<>();
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, UUID.fromString(userId));
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) roles.add(rs.getString(1));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return roles;
  }

  @Override
  public void updateLastLogin(String userId) {
    String sql = "UPDATE security.app_user SET last_login_at = now() WHERE id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, UUID.fromString(userId));
      stmt.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @Override
  public AppUser create(AppUser user) {
    String sql =
        "INSERT INTO security.app_user (company_id, username, email, password_hash, first_name, "
            + "last_name, phone, status) VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS user_status))";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      stmt.setObject(1, user.getCompany_id() == null ? null : UUID.fromString(user.getCompany_id()));
      stmt.setString(2, user.getUsername());
      stmt.setString(3, user.getEmail());
      stmt.setString(4, user.getPassword_hash());
      stmt.setString(5, user.getFirst_name());
      stmt.setString(6, user.getLast_name());
      stmt.setString(7, user.getPhone());
      stmt.setString(8, user.getStatus() == null ? null : user.getStatus().name());

      stmt.executeUpdate();

      try (ResultSet rs = stmt.getGeneratedKeys()) {
        if (rs.next()) {
          user.setId(rs.getString(1));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return user;
  }

  @Override
  public AppUser update(AppUser user) {
    StringBuilder sql = new StringBuilder("UPDATE security.app_user SET ");
    List<Object> parameters = new ArrayList<>();

    if (user.getCompany_id() != null) {
      sql.append("company_id = ?, ");
      parameters.add(UUID.fromString(user.getCompany_id()));
    }
    if (user.getUsername() != null) {
      sql.append("username = ?, ");
      parameters.add(user.getUsername());
    }
    if (user.getEmail() != null) {
      sql.append("email = ?, ");
      parameters.add(user.getEmail());
    }
    if (user.getPassword_hash() != null) {
      sql.append("password_hash = ?, ");
      parameters.add(user.getPassword_hash());
    }
    if (user.getFirst_name() != null) {
      sql.append("first_name = ?, ");
      parameters.add(user.getFirst_name());
    }
    if (user.getLast_name() != null) {
      sql.append("last_name = ?, ");
      parameters.add(user.getLast_name());
    }
    if (user.getPhone() != null) {
      sql.append("phone = ?, ");
      parameters.add(user.getPhone());
    }
    if (user.getStatus() != null) {
      sql.append("status = CAST(? AS user_status), ");
      parameters.add(user.getStatus().name());
    }

    if (parameters.isEmpty()) {
      return user;
    }

    sql.setLength(sql.length() - 2);
    sql.append(" WHERE id = ?");
    parameters.add(UUID.fromString(user.getId()));

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
      for (int index = 0; index < parameters.size(); index++) {
        stmt.setObject(index + 1, parameters.get(index));
      }
      stmt.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return user;
  }

  @Override
  public void delete(String userId) {
    String sql = "DELETE FROM security.app_user WHERE id = ?";
    UUID id = UUID.fromString(userId);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, id);
      stmt.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void assignRole(String userId, String roleId) {
    String sql =
        "INSERT INTO security.user_role (user_id, role_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
    executePair(sql, userId, roleId);
  }

  @Override
  public void removeRole(String userId, String roleId) {
    String sql = "DELETE FROM security.user_role WHERE user_id = ? AND role_id = ?";
    executePair(sql, userId, roleId);
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

  private AppUser mapRowToAppUser(ResultSet rs) throws SQLException {
    return new AppUser(
        rs.getString("id"),
        rs.getString("company_id"),
        rs.getString("username"),
        rs.getString("email"),
        rs.getString("password_hash"),
        rs.getString("first_name"),
        rs.getString("last_name"),
        rs.getString("phone"),
        UserStatus.valueOf(rs.getString("status")),
        rs.getTimestamp("last_login_at"),
        rs.getTimestamp("created_at"),
        rs.getTimestamp("updated_at"));
  }
}
