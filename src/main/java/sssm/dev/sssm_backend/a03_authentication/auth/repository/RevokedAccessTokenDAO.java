package sssm.dev.sssm_backend.a03_authentication.auth.repository;

import java.time.Instant;

public interface RevokedAccessTokenDAO {
  boolean isRevoked(String jti);

  void revoke(String jti, String userId, Instant expiresAt);
}
