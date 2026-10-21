package sssm.dev.sssm_backend.a03_authentication.auth.repository;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenDAO {
  String create(
      String userId, String tokenHash, Instant expiresAt, String userAgent, String ipAddress);

  Optional<RefreshTokenRecord> consumeByHash(String tokenHash);

  void markReplaced(String id, String replacementId);

  void revokeByHash(String tokenHash);
}
