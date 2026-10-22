package sssm.dev.sssm_backend.a03_authentication.auth.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;

@Repository
public class RefreshTokenDAOImpl implements RefreshTokenDAO {

  @Override
  public String create(
      String userId, String tokenHash, Instant expiresAt, String userAgent, String ipAddress) {
    String sql =
        "INSERT INTO security.refresh_token "
            + "(user_id, token_hash, expires_at, user_agent, ip_address) "
            + "VALUES (?, ?, ?, ?, ?) RETURNING id";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, UUID.fromString(userId));
      stmt.setString(2, tokenHash);
      stmt.setTimestamp(3, Timestamp.from(expiresAt));
      stmt.setString(4, truncate(userAgent, 500));
      stmt.setString(5, truncate(ipAddress, 100));
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) return rs.getString(1);
      }
      throw new ServerException("Le refresh token n'a pas pu être enregistré.");
    } catch (SQLException e) {
      throw new ServerException(e);
    }
  }

  @Override
  public Optional<RefreshTokenRecord> consumeByHash(String tokenHash) {
    String sql =
        "UPDATE security.refresh_token SET revoked_at = now() "
            + "WHERE token_hash = ? AND revoked_at IS NULL AND expires_at > now() "
            + "RETURNING id, user_id";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, tokenHash);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) return Optional.of(new RefreshTokenRecord(rs.getString(1), rs.getString(2)));
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return Optional.empty();
  }

  @Override
  public void markReplaced(String id, String replacementId) {
    String sql =
        "UPDATE security.refresh_token SET replaced_by_id = ? "
            + "WHERE id = ? AND revoked_at IS NOT NULL";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, UUID.fromString(replacementId));
      stmt.setObject(2, UUID.fromString(id));
      stmt.executeUpdate();
    } catch (SQLException e) {
      throw new ServerException(e);
    }
  }

  @Override
  public void revokeByHash(String tokenHash) {
    String sql =
        "UPDATE security.refresh_token SET revoked_at = now() "
            + "WHERE token_hash = ? AND revoked_at IS NULL";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, tokenHash);
      stmt.executeUpdate();
    } catch (SQLException e) {
      throw new ServerException(e);
    }
  }

  private static String truncate(String value, int max) {
    if (value == null || value.length() <= max) return value;
    return value.substring(0, max);
  }
}
