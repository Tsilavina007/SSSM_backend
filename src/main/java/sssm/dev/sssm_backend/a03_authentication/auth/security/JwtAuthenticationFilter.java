package sssm.dev.sssm_backend.a03_authentication.auth.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import sssm.dev.sssm_backend.a03_authentication.auth.service.JwtService;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtService jwt;

  public JwtAuthenticationFilter(JwtService jwt) {
    this.jwt = jwt;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      try {
        Claims claims = jwt.parse(header.substring(7));
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        var authorities = roles == null ? List.<SimpleGrantedAuthority>of()
            : roles.stream().map(role -> new SimpleGrantedAuthority(
                role.startsWith("ROLE_") ? role : "ROLE_" + role)).toList();
        var authentication = new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
      } catch (RuntimeException ignored) {
        SecurityContextHolder.clearContext();
      }
    }
    chain.doFilter(request, response);
  }
}
