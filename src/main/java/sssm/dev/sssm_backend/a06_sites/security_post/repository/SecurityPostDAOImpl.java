package sssm.dev.sssm_backend.a06_sites.security_post.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;
import sssm.dev.sssm_backend.a01_common.enums.PostStatus;
import sssm.dev.sssm_backend.a06_sites.security_post.entity.SecurityPost;

public class SecurityPostDAOImpl implements SecurityPostDAO {

  @Override
  public List<SecurityPost> findBySiteId(String siteId) {
    String sql = "SELECT * FROM security.security_post WHERE site_id = ? ORDER BY name";
    List<SecurityPost> posts = new ArrayList<>();
    UUID id = UUID.fromString(siteId);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, id);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          SecurityPost post = mapRowToSecurityPost(rs);
          posts.add(post);
        }
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return posts;
  }

  @Override
  public Optional<SecurityPost> findById(String postId) {
    String sql = "SELECT * FROM security.security_post WHERE id = ?";
    UUID id = UUID.fromString(postId);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, id);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapRowToSecurityPost(rs));
        }
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return Optional.empty();
  }

  @Override
  public SecurityPost create(SecurityPost post) {
    String sql =
        "INSERT INTO security.security_post (site_id, name, code, description, required_agents, "
            + "start_time, end_time, status) VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS security.post_status))";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      stmt.setObject(1, UUID.fromString(post.getSite_id()));
      stmt.setString(2, post.getName());
      stmt.setString(3, post.getCode());
      stmt.setString(4, post.getDescription());
      stmt.setObject(5, post.getRequired_agents() == null ? 1 : post.getRequired_agents());
      stmt.setTime(6, post.getStart_time());
      stmt.setTime(7, post.getEnd_time());
      stmt.setString(
          8, post.getStatus() == null ? PostStatus.ACTIVE.name() : post.getStatus().name());

      stmt.executeUpdate();

      try (ResultSet rs = stmt.getGeneratedKeys()) {
        if (rs.next()) {
          post.setId(rs.getString(1));
        }
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return post;
  }

  @Override
  public SecurityPost update(SecurityPost post) {
    StringBuilder sql = new StringBuilder("UPDATE security.security_post SET ");
    List<Object> parameters = new ArrayList<>();

    if (post.getSite_id() != null) {
      sql.append("site_id = ?, ");
      parameters.add(UUID.fromString(post.getSite_id()));
    }
    if (post.getName() != null) {
      sql.append("name = ?, ");
      parameters.add(post.getName());
    }
    if (post.getCode() != null) {
      sql.append("code = ?, ");
      parameters.add(post.getCode());
    }
    if (post.getDescription() != null) {
      sql.append("description = ?, ");
      parameters.add(post.getDescription());
    }
    if (post.getRequired_agents() != null) {
      sql.append("required_agents = ?, ");
      parameters.add(post.getRequired_agents());
    }
    if (post.getStart_time() != null) {
      sql.append("start_time = ?, ");
      parameters.add(post.getStart_time());
    }
    if (post.getEnd_time() != null) {
      sql.append("end_time = ?, ");
      parameters.add(post.getEnd_time());
    }
    if (post.getStatus() != null) {
      sql.append("status = CAST(? AS security.post_status), ");
      parameters.add(post.getStatus().name());
    }

    if (parameters.isEmpty()) {
      return post;
    }

    sql.setLength(sql.length() - 2);
    sql.append(" WHERE id = ?");
    parameters.add(UUID.fromString(post.getId()));

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
      for (int index = 0; index < parameters.size(); index++) {
        stmt.setObject(index + 1, parameters.get(index));
      }
      stmt.executeUpdate();
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return post;
  }

  @Override
  public void delete(String postId) {
    String sql = "DELETE FROM security.security_post WHERE id = ?";
    UUID id = UUID.fromString(postId);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, id);
      stmt.executeUpdate();
    } catch (SQLException e) {
      throw new ServerException(e);
    }
  }

  private SecurityPost mapRowToSecurityPost(ResultSet rs) throws SQLException {
    return new SecurityPost(
        rs.getString("id"),
        rs.getString("site_id"),
        rs.getString("name"),
        rs.getString("code"),
        rs.getString("description"),
        rs.getInt("required_agents"),
        rs.getTime("start_time"),
        rs.getTime("end_time"),
        PostStatus.valueOf(rs.getString("status")),
        rs.getTimestamp("created_at"));
  }
}
