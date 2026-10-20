package sssm.dev.sssm_backend.a03_authentication;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.Filter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a03_authentication.auth.service.JwtService;

/** Vérifie l'endpoint whoami et son rattachement au JWT courant. */
@SpringBootTest
class WhoAmIApiTests {

  @Autowired private WebApplicationContext context;
  @Autowired private JwtService jwtService;

  private MockMvc mockMvc;
  private String username;
  private String email;
  private String password;

  @BeforeEach
  void setUp() throws Exception {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
            .build();
    String marker = "whoami-test-" + System.nanoTime();
    username = marker;
    email = marker + "@example.test";
    password = "change-me-123";
    cleanup();
  }

  @AfterEach
  void cleanUp() throws Exception {
    cleanup();
  }

  @Test
  void whoAmIReturnsCurrentUser() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"username\":\""
                        + username
                        + "\",\"email\":\""
                        + email
                        + "\",\"password\":\""
                        + password
                        + "\"}"))
        .andExpect(status().isCreated());

    MvcResult login =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"login\":\"" + username + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value(notNullValue()))
            .andReturn();

    String token = JsonPath.read(login.getResponse().getContentAsString(), "$.token");

    mockMvc
        .perform(get("/api/auth/whoami").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(notNullValue()))
        .andExpect(jsonPath("$.username").value(username))
        .andExpect(jsonPath("$.email").value(email))
        .andExpect(jsonPath("$.roles").isArray());
  }

  @Test
  void whoAmIWithoutTokenReturns401() throws Exception {
    mockMvc
        .perform(get("/api/auth/whoami"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errors.message").value("Authentification requise"))
        .andExpect(jsonPath("$.errors.detail").value(containsString("Authorization")))
        .andExpect(jsonPath("$.errors.code").value("UNAUTHORIZED"));
  }

  @Test
  void whoAmIWithUnknownUserReturns404() throws Exception {
    String unknownUsername = "whoami-ghost-" + System.nanoTime();
    String token =
        jwtService.createToken(
            UUID.randomUUID().toString(), unknownUsername, List.of("USER"));

    mockMvc
        .perform(get("/api/auth/whoami").header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errors.message").value("Utilisateur authentifié introuvable"))
        .andExpect(jsonPath("$.errors.detail").value(containsString(unknownUsername)))
        .andExpect(jsonPath("$.errors.code").value("NOT_FOUND"));
  }

  private void cleanup() throws Exception {
    try (Connection conn = DatabaseConnection.getConnection()) {
      try (PreparedStatement stmt =
          conn.prepareStatement(
              "DELETE FROM security.user_role WHERE user_id IN "
                  + "(SELECT id FROM security.app_user WHERE username LIKE 'whoami-test-%')")) {
        stmt.executeUpdate();
      }
      try (PreparedStatement stmt =
          conn.prepareStatement(
              "DELETE FROM security.app_user WHERE username LIKE 'whoami-test-%'")) {
        stmt.executeUpdate();
      }
    }
  }
}
