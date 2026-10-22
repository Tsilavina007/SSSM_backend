package sssm.dev.sssm_backend.a03_authentication.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import sssm.dev.sssm_backend.a01_common.errors.ItemError;
import sssm.dev.sssm_backend.a03_authentication.auth.repository.RevokedAccessTokenDAO;
import sssm.dev.sssm_backend.a03_authentication.auth.service.JwtService;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtService jwt;
  private final RevokedAccessTokenDAO revokedAccessTokens;
  private final ObjectMapper objectMapper;

  public JwtAuthenticationFilter(
      JwtService jwt, RevokedAccessTokenDAO revokedAccessTokens, ObjectMapper objectMapper) {
    this.jwt = jwt;
    this.revokedAccessTokens = revokedAccessTokens;
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      try {
        Claims claims = jwt.parse(header.substring(7));
        String jti = claims.getId();
        if (jti == null || jti.isBlank() || revokedAccessTokens.isRevoked(jti)) {
          throw new JwtException("Le jeton JWT a été révoqué.");
        }

        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        @SuppressWarnings("unchecked")
        List<String> permissions = claims.get("permissions", List.class);
        boolean platformAdmin = Boolean.TRUE.equals(claims.get("platform_admin", Boolean.class));

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (roles != null) {
          roles.stream()
              .filter(role -> role != null && !role.isBlank())
              .map(
                  role ->
                      new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role))
              .forEach(authorities::add);
        }
        if (permissions != null) {
          permissions.stream()
              .filter(permission -> permission != null && !permission.isBlank())
              .map(SimpleGrantedAuthority::new)
              .forEach(authorities::add);
        }
        if (platformAdmin) {
          authorities.add(new SimpleGrantedAuthority("PLATFORM_ADMIN"));
        }

        JwtPrincipal principal =
            new JwtPrincipal(
                claims.get("uid", String.class),
                claims.getSubject(),
                claims.get("company_id", String.class),
                platformAdmin);
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
      } catch (RuntimeException e) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), Map.of("errors", jwtError(e)));
        return;
      }
    }
    chain.doFilter(request, response);
  }

  private static ItemError jwtError(RuntimeException exception) {
    if (exception instanceof ExpiredJwtException) {
      return new ItemError(
          "Jeton expiré",
          "Le jeton JWT a expiré. Connectez-vous à nouveau pour obtenir un nouveau jeton.",
          "UNAUTHORIZED");
    }
    if (exception instanceof MalformedJwtException) {
      return new ItemError(
          "Jeton mal formé", "Le jeton JWT n'est pas au format attendu.", "UNAUTHORIZED");
    }
    if (exception instanceof SignatureException) {
      return new ItemError(
          "Signature du jeton invalide",
          "La signature du jeton JWT ne correspond pas à la clé du serveur.",
          "UNAUTHORIZED");
    }
    if (exception instanceof UnsupportedJwtException) {
      return new ItemError(
          "Jeton non supporté",
          "Le type ou les algorithmes du jeton JWT ne sont pas supportés.",
          "UNAUTHORIZED");
    }
    if (exception instanceof JwtException) {
      return new ItemError(
          "Jeton invalide",
          textOr(exception.getMessage(), "Le jeton JWT est invalide ou a expiré."),
          "UNAUTHORIZED");
    }
    return new ItemError(
        "Jeton invalide",
        textOr(
            exception.getMessage(), "Le jeton JWT est vide ou contient des caractères invalides."),
        "UNAUTHORIZED");
  }

  private static String textOr(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }
}
