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

/** Vérifie le refresh token distinct, sa rotation, sa révocation et le logout. */
@SpringBootTest
class RefreshTokenApiTests {

  @Autowired private WebApplicationContext context;
  @Autowired private JwtService jwtService;

  private MockMvc mockMvc;
  private String adminToken;
  private String username;
  private String email;
  private String password;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
            .build();
    adminToken = jwtService.createToken("test-admin", "refresh-admin", List.of("ADMIN"));
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
  void refreshReturnsNewAccessAndRefreshTokens() throws Exception {
    Tokens tokens = createUserAndLogin();

    MvcResult result =
        mockMvc
            .perform(
                post("/api/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"refreshToken\":\"" + tokens.refreshToken() + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value(notNullValue()))
            .andExpect(jsonPath("$.refreshToken").value(notNullValue()))
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
  void refreshRotatesAndRevokesOldRefreshToken() throws Exception {
    Tokens tokens = createUserAndLogin();

    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + tokens.refreshToken() + "\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + tokens.refreshToken() + "\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errors.code").value("UNAUTHORIZED"));
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
                .content("{\"refreshToken\":\"not-a-jwt\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errors.code").value("UNAUTHORIZED"));
  }

  @Test
  void logoutRevokesRefreshAndAccessTokens() throws Exception {
    Tokens tokens = createUserAndLogin();

    mockMvc
        .perform(
            post("/api/auth/logout")
                .header("Authorization", "Bearer " + tokens.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + tokens.refreshToken() + "\"}"))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            get("/api/auth/whoami")
                .header("Authorization", "Bearer " + tokens.accessToken()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errors.code").value("UNAUTHORIZED"));

    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + tokens.refreshToken() + "\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errors.code").value("UNAUTHORIZED"));
  }

  private Tokens createUserAndLogin() throws Exception {
    mockMvc
        .perform(
            post("/api/users")
                .header("Authorization", "Bearer " + adminToken)
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
            .andExpect(jsonPath("$.refreshToken").value(notNullValue()))
            .andReturn();
    String body = login.getResponse().getContentAsString();
    return new Tokens(
        JsonPath.read(body, "$.token"), JsonPath.read(body, "$.refreshToken"));
  }

  private record Tokens(String accessToken, String refreshToken) {}
}
