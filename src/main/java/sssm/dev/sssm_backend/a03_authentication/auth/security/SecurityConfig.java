package sssm.dev.sssm_backend.a03_authentication.auth.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

import sssm.dev.sssm_backend.a01_common.errors.ItemError;
import sssm.dev.sssm_backend.a01_common.errors.handler.ProblemResponseWriter;

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
      HttpSecurity http,
      JwtAuthenticationFilter jwtFilter,
      ObjectMapper objectMapper,
      CorsConfigurationSource corsConfigurationSource)
      throws Exception {
    http.cors(cors -> cors.configurationSource(corsConfigurationSource))
        .csrf(csrf -> csrf.disable())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/",
                        "/ping",
                        "/error",
                        "/api/v1/auth/login",
                        "/api/v1/auth/refresh",
                        "/api/v1/auth/logout")
                    .permitAll()
                    .requestMatchers("/api/v1/auth/register")
                    .hasAnyAuthority("ROLE_ADMIN", "users.manage")
                    .requestMatchers("/api/v1/inventory/**")
                    .hasAnyAuthority("ROLE_ADMIN", "ROLE_INVENTORY", "inventory.manage")
                    .requestMatchers("/api/v1/users/**")
                    .hasAnyAuthority("ROLE_ADMIN", "users.manage")
                    .requestMatchers("/api/v1/roles/**")
                    .hasAnyAuthority("ROLE_ADMIN", "roles.manage")
                    .requestMatchers("/api/v1/permissions/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            errors ->
                errors
                    .authenticationEntryPoint(
                        (request, response, exception) ->
                            writeError(
                                request,
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
                                request,
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

  @Bean
  CorsConfigurationSource corsConfigurationSource(
      @Value(
              "${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173,http://localhost:4200}")
          String allowedOrigins) {
    List<String> origins =
        Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isBlank())
            .toList();
    if (origins.isEmpty()) {
      throw new IllegalStateException(
          "app.cors.allowed-origins doit contenir au moins une origine explicite.");
    }

    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(origins);
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  private static void writeError(
      jakarta.servlet.http.HttpServletRequest request,
      HttpServletResponse response,
      ObjectMapper objectMapper,
      int status,
      ItemError error)
      throws IOException {
    ProblemResponseWriter.write(
        response, objectMapper, status, error, request.getRequestURI());
  }
}
