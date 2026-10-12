package sssm.dev.sssm_backend.a06_sites.post_requirement.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a06_sites.post_requirement.entity.PostRequirement;

public class PostRequirementDAOImpl implements PostRequirementDAO {

  @Override
  public List<PostRequirement> findByPostId(String postId) {
    String sql = "SELECT * FROM security.post_requirement WHERE post_id = ? ORDER BY requirement";
    List<PostRequirement> requirements = new ArrayList<>();
    UUID id = UUID.fromString(postId);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, id);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          PostRequirement requirement = mapRowToPostRequirement(rs);
          requirements.add(requirement);
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return requirements;
  }

  @Override
  public Optional<PostRequirement> findById(String requirementId) {
    String sql = "SELECT * FROM security.post_requirement WHERE id = ?";
    UUID id = UUID.fromString(requirementId);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, id);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapRowToPostRequirement(rs));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }

  @Override
  public PostRequirement create(PostRequirement requirement) {
    String sql = "INSERT INTO security.post_requirement (post_id, requirement) VALUES (?, ?)";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      stmt.setObject(1, UUID.fromString(requirement.getPost_id()));
      stmt.setString(2, requirement.getRequirement());
      stmt.executeUpdate();

      try (ResultSet rs = stmt.getGeneratedKeys()) {
        if (rs.next()) {
          requirement.setId(rs.getString(1));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return requirement;
  }

  @Override
  public PostRequirement update(PostRequirement requirement) {
    StringBuilder sql = new StringBuilder("UPDATE security.post_requirement SET ");
    List<Object> parameters = new ArrayList<>();

    if (requirement.getPost_id() != null) {
      sql.append("post_id = ?, ");
      parameters.add(UUID.fromString(requirement.getPost_id()));
    }
    if (requirement.getRequirement() != null) {
      sql.append("requirement = ?, ");
      parameters.add(requirement.getRequirement());
    }

    if (parameters.isEmpty()) {
      return requirement;
    }

    sql.setLength(sql.length() - 2);
    sql.append(" WHERE id = ?");
    parameters.add(UUID.fromString(requirement.getId()));

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
      for (int index = 0; index < parameters.size(); index++) {
        stmt.setObject(index + 1, parameters.get(index));
      }
      stmt.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return requirement;
  }

  @Override
  public void delete(String requirementId) {
    String sql = "DELETE FROM security.post_requirement WHERE id = ?";
    UUID id = UUID.fromString(requirementId);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, id);
      stmt.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private PostRequirement mapRowToPostRequirement(ResultSet rs) throws SQLException {
    return new PostRequirement(
        rs.getString("id"), rs.getString("post_id"), rs.getString("requirement"));
  }
}
