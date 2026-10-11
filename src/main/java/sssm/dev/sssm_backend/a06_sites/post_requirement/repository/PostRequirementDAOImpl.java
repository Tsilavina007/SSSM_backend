package sssm.dev.sssm_backend.a06_sites.post_requirement.repository;

import java.sql.*;
import java.util.*;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a06_sites.post_requirement.entity.PostRequirement;

public class PostRequirementDAOImpl implements PostRequirementDAO {
    public List<PostRequirement> findByPostId(String postId) {
        List<PostRequirement> result = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM security.post_requirement WHERE post_id = ? ORDER BY requirement")) {
            statement.setObject(1, uuid(postId));
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) result.add(map(rows));
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return result;
    }

    public Optional<PostRequirement> findById(String id) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM security.post_requirement WHERE id = ?")) {
            statement.setObject(1, uuid(id));
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(map(rows)) : Optional.empty();
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
            return Optional.empty();
        }
    }

    public PostRequirement create(PostRequirement requirement) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("INSERT INTO security.post_requirement(post_id, requirement) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            statement.setObject(1, uuid(requirement.getPost_id()));
            statement.setString(2, requirement.getRequirement());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) requirement.setId(keys.getString(1));
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return requirement;
    }

    public PostRequirement update(PostRequirement requirement) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE security.post_requirement SET requirement = ? WHERE id = ?")) {
            statement.setString(1, requirement.getRequirement());
            statement.setObject(2, uuid(requirement.getId()));
            statement.executeUpdate();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return requirement;
    }

    public void delete(String id) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM security.post_requirement WHERE id = ?")) {
            statement.setObject(1, uuid(id));
            statement.executeUpdate();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    private UUID uuid(String value) { return value == null ? null : UUID.fromString(value); }
    private PostRequirement map(ResultSet rows) throws SQLException {
        return new PostRequirement(rows.getString("id"), rows.getString("post_id"), rows.getString("requirement"));
    }
}
