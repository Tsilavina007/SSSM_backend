package sssm.dev.sssm_backend.a03_authentication.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final SecretKey key;
  private final long expirationMs;
  private final String issuer;
  private final String audience;

  public JwtService(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.expiration-ms:3600000}") long expirationMs,
      @Value("${app.jwt.issuer:sssm-api}") String issuer,
      @Value("${app.jwt.audience:sssm-client}") String audience) {
    if (secret.length() < 32)
      throw new IllegalArgumentException(
          "Le secret JWT (app.jwt.secret) doit contenir au moins 32 caractères.");
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expirationMs = expirationMs;
    this.issuer = issuer;
    this.audience = audience;
  }

  /**
   * Compatibilité avec les tests et les outils qui ne portent pas encore de contexte entreprise.
   * Les nouveaux flux doivent utiliser {@link #createAccessToken}.
   */
  public String createToken(String userId, String username, List<String> roles) {
    List<String> safeRoles = roles == null ? List.of() : roles;
    boolean platformAdmin = safeRoles.stream().anyMatch(role -> "ADMIN".equalsIgnoreCase(role));
    return createAccessToken(userId, username, null, safeRoles, List.of(), platformAdmin);
  }

  public String createAccessToken(
      String userId,
      String username,
      String companyId,
      List<String> roles,
      List<String> permissions,
      boolean platformAdmin) {
    Date now = new Date();
    return Jwts.builder()
        .id(UUID.randomUUID().toString())
        .issuer(issuer)
        .audience()
        .add(audience)
        .and()
        .subject(username)
        .claim("uid", userId)
        .claim("company_id", companyId)
        .claim("roles", roles == null ? List.of() : roles)
        .claim("permissions", permissions == null ? List.of() : permissions)
        .claim("platform_admin", platformAdmin)
        .issuedAt(now)
        .expiration(new Date(now.getTime() + expirationMs))
        .signWith(key)
        .compact();
  }

  public Claims parse(String token) {
    return Jwts.parser()
        .verifyWith(key)
        .requireIssuer(issuer)
        .requireAudience(audience)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }
}
