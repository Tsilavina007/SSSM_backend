package sssm.dev.sssm_backend.a03_authentication;

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

/** Vérifie l'endpoint POST /api/auth/refresh et ses réponses d'erreur. */
@SpringBootTest
class RefreshTokenApiTests {

  @Autowired private WebApplicationContext context;
  @Autowired private JwtService jwtService;

  private MockMvc mockMvc;
  private String username;
  private String email;
  private String password;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
            .build();
    String marker = "refresh-test-" + System.nanoTime();
    username = marker;
    email = marker + "@example.test";
    password = "change-me-123";
  }

  @AfterEach
  void cleanUp() throws Exception {
    try (Connection conn = DatabaseConnection.getConnection()) {
      try (PreparedStatement stmt =
          conn.prepareStatement(
              "DELETE FROM security.user_role WHERE user_id IN "
                  + "(SELECT id FROM security.app_user WHERE username LIKE 'refresh-test-%')")) {
        stmt.executeUpdate();
      }
      try (PreparedStatement stmt =
          conn.prepareStatement(
              "DELETE FROM security.app_user WHERE username LIKE 'refresh-test-%'")) {
        stmt.executeUpdate();
      }
    }
  }

  @Test
  void refreshWithoutAuthorizationHeaderReturnsNewToken() throws Exception {
    String token = registerAndLogin();

    MvcResult result =
        mockMvc
            .perform(
                post("/api/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"token\":\"" + token + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value(notNullValue()))
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.user.username").value(username))
            .andReturn();

    String refreshed = JsonPath.read(result.getResponse().getContentAsString(), "$.token");

    mockMvc
        .perform(get("/api/auth/whoami").header("Authorization", "Bearer " + refreshed))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value(username));
  }

  @Test
  void refreshWithoutTokenReturnsBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.code").value("CLIENT_ERROR"));
  }

  @Test
  void refreshWithInvalidTokenReturnsUnauthorized() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"not-a-jwt\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errors.code").value("UNAUTHORIZED"));
  }

  @Test
  void refreshWithUnknownUserReturnsNotFound() throws Exception {
    String ghostToken =
        jwtService.createToken(
            UUID.randomUUID().toString(),
            "refresh-ghost-" + System.nanoTime(),
            List.of("USER"));

    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + ghostToken + "\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errors.code").value("NOT_FOUND"));
  }

  private String registerAndLogin() throws Exception {
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
            .andReturn();
    return JsonPath.read(login.getResponse().getContentAsString(), "$.token");
  }
}
