package sssm.dev.sssm_backend.a03_authentication;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.Filter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

/** Vérifie que la création d'un utilisateur utilise un DTO et hache le mot de passe. */
@SpringBootTest
class AppUserApiTests {

  private static final String SUPPLIED_ID = "00000000-0000-0000-0000-000000000001";

  @Autowired private WebApplicationContext context;
  @Autowired private JwtService jwtService;

  private MockMvc mockMvc;
  private String adminToken;
  private String username;
  private String email;
  private String password;

  @BeforeEach
  void setUp() throws Exception {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
            .build();
    adminToken = jwtService.createToken("test-admin", "user-api-admin", List.of("ADMIN"));
    String marker = "user-api-test-" + System.nanoTime();
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
  void createUserUsesDtoAndHashesPassword() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/users")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createPayload()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(not(SUPPLIED_ID)))
            .andExpect(jsonPath("$.username").value(username))
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.password_hash").doesNotExist())
            .andReturn();

    String createdId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    assertNotEquals(SUPPLIED_ID, createdId);

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"" + username + "\",\"password\":\"" + password + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value(notNullValue()));
  }

  @Test
  void updateUserIgnoresProtectedFields() throws Exception {
    MvcResult created =
        mockMvc
            .perform(
                post("/api/users")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createPayload()))
            .andExpect(status().isCreated())
            .andReturn();

    String createdId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(
            patch("/api/users/" + createdId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "username": "hacked-%s",
                      "company_id": "00000000-0000-0000-0000-000000000002",
                      "password_hash": "injected-hash",
                      "first_name": "Updated"
                    }
                    """
                        .formatted(username)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.first_name").value("Updated"));

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt =
            conn.prepareStatement(
                "SELECT username, company_id, password_hash, first_name "
                    + "FROM security.app_user WHERE id = ?")) {
      stmt.setObject(1, UUID.fromString(createdId));
      try (ResultSet rs = stmt.executeQuery()) {
        assertTrue(rs.next());
        assertEquals(username, rs.getString("username"));
        assertNull(rs.getObject("company_id"));
        assertNotEquals("injected-hash", rs.getString("password_hash"));
        assertEquals("Updated", rs.getString("first_name"));
      }
    }
  }

  @Test
  void createUserWithoutRequiredFieldsReturns400() throws Exception {
    mockMvc
        .perform(
            post("/api/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.message").value("Validation échouée"))
        .andExpect(jsonPath("$.errors.detail").value(containsString("username")))
        .andExpect(jsonPath("$.errors.detail").value(containsString("email")))
        .andExpect(jsonPath("$.errors.detail").value(containsString("password")))
        .andExpect(jsonPath("$.errors.code").value("VALIDATION_ERROR"));
  }

  private String createPayload() {
    return """
        {
          "id": "%s",
          "password_hash": "not-a-real-hash",
          "username": "%s",
          "email": "%s",
          "password": "%s",
          "first_name": "Api",
          "last_name": "Test",
          "phone": "0340000000"
        }
        """
        .formatted(SUPPLIED_ID, username, email, password);
  }

  private void cleanup() throws Exception {
    try (Connection conn = DatabaseConnection.getConnection()) {
      try (PreparedStatement stmt =
          conn.prepareStatement(
              "DELETE FROM security.user_role WHERE user_id IN "
                  + "(SELECT id FROM security.app_user WHERE username LIKE 'user-api-test-%')")) {
        stmt.executeUpdate();
      }
      try (PreparedStatement stmt =
          conn.prepareStatement(
              "DELETE FROM security.app_user WHERE username LIKE 'user-api-test-%'")) {
        stmt.executeUpdate();
      }
    }
  }
}
