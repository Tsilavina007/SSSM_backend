package sssm.dev.sssm_backend.a03_authentication.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final SecretKey key;
  private final long expirationMs;

  public JwtService(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.expiration-ms:3600000}") long expirationMs) {
    if (secret.length() < 32) throw new IllegalArgumentException("app.jwt.secret must be at least 32 characters");
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expirationMs = expirationMs;
  }

  public String createToken(String userId, String username, List<String> roles) {
    Date now = new Date();
    return Jwts.builder().subject(username).claim("uid", userId).claim("roles", roles)
        .issuedAt(now).expiration(new Date(now.getTime() + expirationMs)).signWith(key).compact();
  }

  public Claims parse(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }

  public SecretKey getKey() {
    return key;
  }
}
