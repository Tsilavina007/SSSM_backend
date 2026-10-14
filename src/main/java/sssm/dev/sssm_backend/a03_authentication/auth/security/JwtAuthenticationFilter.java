package sssm.dev.sssm_backend.a03_authentication.auth.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import sssm.dev.sssm_backend.a01_common.errors.mapper.ErrorMapper;
import sssm.dev.sssm_backend.a03_authentication.auth.service.JwtService;
import tools.jackson.databind.ObjectMapper;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtService jwt;
  private final ObjectMapper objectMapper;

  public JwtAuthenticationFilter(JwtService jwt, ObjectMapper objectMapper) {
    this.jwt = jwt;
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
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        var authorities =
            roles == null
                ? List.<SimpleGrantedAuthority>of()
                : roles.stream()
                    .map(
                        role ->
                            new SimpleGrantedAuthority(
                                role.startsWith("ROLE_") ? role : "ROLE_" + role))
                    .toList();
        var authentication =
            new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
      } catch (RuntimeException e) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(
            response.getWriter(),
            Map.of("errors", ErrorMapper.toItemError(e))
        );
        return;
      }
    }
    chain.doFilter(request, response);
  }
}
