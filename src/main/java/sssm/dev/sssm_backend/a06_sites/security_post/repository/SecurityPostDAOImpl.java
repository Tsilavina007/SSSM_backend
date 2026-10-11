package sssm.dev.sssm_backend.a06_sites.security_post.repository;

import java.sql.*;
import java.util.*;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a01_common.enums.PostStatus;
import sssm.dev.sssm_backend.a06_sites.security_post.entity.SecurityPost;

public class SecurityPostDAOImpl implements SecurityPostDAO {
  public List<SecurityPost> findBySiteId(String siteId) {
    List<SecurityPost> result = new ArrayList<>();
    try (Connection connection = DatabaseConnection.getConnection();
        PreparedStatement statement =
            connection.prepareStatement(
                "SELECT * FROM security.security_post WHERE site_id = ? ORDER BY name")) {
      statement.setObject(1, uuid(siteId));
      try (ResultSet rows = statement.executeQuery()) {
        while (rows.next()) result.add(map(rows));
      }
    } catch (SQLException exception) {
      exception.printStackTrace();
    }
    return result;
  }

  public Optional<SecurityPost> findById(String id) {
    try (Connection connection = DatabaseConnection.getConnection();
        PreparedStatement statement =
            connection.prepareStatement("SELECT * FROM security.security_post WHERE id = ?")) {
      statement.setObject(1, uuid(id));
      try (ResultSet rows = statement.executeQuery()) {
        return rows.next() ? Optional.of(map(rows)) : Optional.empty();
      }
    } catch (SQLException exception) {
      exception.printStackTrace();
      return Optional.empty();
    }
  }

  public SecurityPost create(SecurityPost post) {
    String sql =
        "INSERT INTO security.security_post(site_id, name, code, description, required_agents, start_time, end_time, status) VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS post_status))";
    try (Connection connection = DatabaseConnection.getConnection();
        PreparedStatement statement =
            connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      values(statement, post);
      statement.executeUpdate();
      try (ResultSet keys = statement.getGeneratedKeys()) {
        if (keys.next()) post.setId(keys.getString(1));
      }
    } catch (SQLException exception) {
      exception.printStackTrace();
    }
    return post;
  }

  public SecurityPost update(SecurityPost post) {
    String sql =
        "UPDATE security.security_post SET site_id = ?, name = ?, code = ?, description = ?, required_agents = ?, start_time = ?, end_time = ?, status = COALESCE(CAST(? AS post_status), status) WHERE id = ?";
    try (Connection connection = DatabaseConnection.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      values(statement, post);
      statement.setObject(9, uuid(post.getId()));
      statement.executeUpdate();
    } catch (SQLException exception) {
      exception.printStackTrace();
    }
    return post;
  }

  private void values(PreparedStatement statement, SecurityPost post) throws SQLException {
    statement.setObject(1, uuid(post.getSite_id()));
    statement.setString(2, post.getName());
    statement.setString(3, post.getCode());
    statement.setString(4, post.getDescription());
    statement.setInt(5, post.getRequired_agents() == null ? 1 : post.getRequired_agents());
    statement.setTime(6, post.getStart_time());
    statement.setTime(7, post.getEnd_time());
    statement.setString(
        8, post.getStatus() == null ? PostStatus.ACTIVE.name() : post.getStatus().name());
  }

  public void delete(String id) {
    try (Connection connection = DatabaseConnection.getConnection();
        PreparedStatement statement =
            connection.prepareStatement("DELETE FROM security.security_post WHERE id = ?")) {
      statement.setObject(1, uuid(id));
      statement.executeUpdate();
    } catch (SQLException exception) {
      exception.printStackTrace();
    }
  }

  private UUID uuid(String value) {
    return value == null ? null : UUID.fromString(value);
  }

  private SecurityPost map(ResultSet rows) throws SQLException {
    return new SecurityPost(
        rows.getString("id"),
        rows.getString("site_id"),
        rows.getString("name"),
        rows.getString("code"),
        rows.getString("description"),
        rows.getInt("required_agents"),
        rows.getTime("start_time"),
        rows.getTime("end_time"),
        PostStatus.valueOf(rows.getString("status")),
        rows.getTimestamp("created_at"));
  }
}
