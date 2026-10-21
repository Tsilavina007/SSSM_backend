package sssm.dev.sssm_backend.a03_authentication.auth.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;

@Repository
public class RevokedAccessTokenDAOImpl implements RevokedAccessTokenDAO {

  @Override
  public boolean isRevoked(String jti) {
    String sql = "SELECT 1 FROM security.revoked_access_token WHERE jti = ?";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, UUID.fromString(jti));
      try (ResultSet rs = stmt.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
  }

  @Override
  public void revoke(String jti, String userId, Instant expiresAt) {
    String sql =
        "INSERT INTO security.revoked_access_token (jti, user_id, expires_at) "
            + "VALUES (?, ?, ?) ON CONFLICT (jti) DO NOTHING";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, UUID.fromString(jti));
      stmt.setObject(2, userId == null ? null : UUID.fromString(userId));
      stmt.setTimestamp(3, Timestamp.from(expiresAt));
      stmt.executeUpdate();
    } catch (SQLException e) {
      throw new ServerException(e);
    }
  }
}
