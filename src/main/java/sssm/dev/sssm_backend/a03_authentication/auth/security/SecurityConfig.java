package sssm.dev.sssm_backend.a03_authentication.auth.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

import sssm.dev.sssm_backend.a01_common.errors.ItemError;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  UserDetailsService unusedFormLoginUserDetailsService() {
    return username -> {
      throw new UsernameNotFoundException("JWT authentication is required");
    };
  }

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http, JwtAuthenticationFilter jwtFilter, ObjectMapper objectMapper)
      throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/", "/ping", "/error", "/api/auth/register", "/api/auth/login")
                    .permitAll()
                    .requestMatchers("/api/inventory/**")
                    .authenticated()
                    .requestMatchers("/api/users/**", "/api/roles/**", "/api/permissions/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            errors ->
                errors
                    .authenticationEntryPoint(
                        (request, response, exception) ->
                            writeError(
                                response,
                                objectMapper,
                                HttpServletResponse.SC_UNAUTHORIZED,
                                new ItemError(
                                    "Authentification requise",
                                    "Aucun jeton JWT valide n'a été fourni. Ajoutez l'en-tête "
                                        + "'Authorization: Bearer <token>'.",
                                    "UNAUTHORIZED")))
                    .accessDeniedHandler(
                        (request, response, exception) ->
                            writeError(
                                response,
                                objectMapper,
                                HttpServletResponse.SC_FORBIDDEN,
                                new ItemError(
                                    "Accès refusé",
                                    "Le jeton JWT est valide, mais le rôle associé ne permet pas "
                                        + "d'accéder à cette ressource.",
                                    "FORBIDDEN"))))
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  private static void writeError(
      HttpServletResponse response, ObjectMapper objectMapper, int status, ItemError error)
      throws IOException {
    response.setStatus(status);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    objectMapper.writeValue(response.getWriter(), Map.of("errors", error));
  }
}
