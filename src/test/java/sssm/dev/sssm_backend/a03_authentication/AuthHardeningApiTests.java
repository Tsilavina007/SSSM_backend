package sssm.dev.sssm_backend.a03_authentication;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.jsonwebtoken.Claims;
import jakarta.servlet.Filter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

/** Vérifie l'inscription fermée, le scope entreprise et le RBAC des routes sensibles. */
@SpringBootTest
class AuthHardeningApiTests {

  private static final String OTHER_COMPANY = "00000000-0000-0000-0000-000000000099";

  @Autowired private WebApplicationContext context;
  @Autowired private JwtService jwtService;

  private MockMvc mockMvc;
  private String username;
  private String email;
  private String password;
  private String companyId;

  @BeforeEach
  void setUp() throws Exception {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
            .build();
    companyId = singleCompanyId();
    String marker = "reg-test-" + System.nanoTime();
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
  void publicRegistrationIsDisabled() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerPayload()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errors.code").value("UNAUTHORIZED"));
  }

  @Test
  void userWithoutAdminPermissionCannotRegister() throws Exception {
    String userToken =
        jwtService.createToken("test-user", "register-user", List.of("USER"));

    mockMvc
        .perform(
            post("/api/auth/register")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerPayload()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errors.code").value("FORBIDDEN"));
  }

  @Test
  void companyAdminCanRegisterOnlyInsideOwnCompanyAndTokenCarriesTenant() throws Exception {
    String companyAdminToken =
        jwtService.createAccessToken(
            "test-company-admin",
            "company-admin-test",
            companyId,
            List.of("COMPANY_ADMIN"),
            List.of("users.manage"),
            false);

    mockMvc
        .perform(
            post("/api/auth/register")
                .header("Authorization", "Bearer " + companyAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerPayload()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.user.companyId").value(companyId))
        .andExpect(jsonPath("$.user.username").value(username));

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

    String accessToken = JsonPath.read(login.getResponse().getContentAsString(), "$.token");
    Claims claims = jwtService.parse(accessToken);
    assertEquals(companyId, claims.get("company_id", String.class));
    assertEquals("sssm-api", claims.getIssuer());
    assertEquals("sssm-client", claims.getAudience().iterator().next());
    assertNotNull(claims.getId());
    assertNotNull(claims.get("permissions", List.class));
  }

  @Test
  void companyAdminCannotReadUserFromAnotherCompany() throws Exception {
    String otherCompanyAdminToken =
        jwtService.createAccessToken(
            "test-other-company-admin",
            "other-company-admin",
            OTHER_COMPANY,
            List.of("COMPANY_ADMIN"),
            List.of("users.manage"),
            false);

    mockMvc
        .perform(
            get("/api/users/" + existingUserId())
                .header("Authorization", "Bearer " + otherCompanyAdminToken))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errors.code").value("NOT_FOUND"));
  }

  @Test
  void companyAdminListIsScopedToOwnCompany() throws Exception {
    String otherCompanyAdminToken =
        jwtService.createAccessToken(
            "test-other-company-admin",
            "other-company-admin",
            OTHER_COMPANY,
            List.of("COMPANY_ADMIN"),
            List.of("users.manage"),
            false);

    mockMvc
        .perform(
            get("/api/users")
                .header("Authorization", "Bearer " + otherCompanyAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void companyAdminCannotManageGlobalPermissionCatalog() throws Exception {
    String companyAdminToken =
        jwtService.createAccessToken(
            "test-company-admin",
            "company-admin-test",
            companyId,
            List.of("COMPANY_ADMIN"),
            List.of("users.manage", "roles.manage"),
            false);

    mockMvc
        .perform(
            get("/api/permissions")
                .header("Authorization", "Bearer " + companyAdminToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errors.code").value("FORBIDDEN"));
  }

  @Test
  void inventoryRequiresInventoryPermission() throws Exception {
    String userToken = jwtService.createToken("test-user", "inventory-user", List.of("USER"));
    mockMvc
        .perform(
            get("/api/inventory/items")
                .header("Authorization", "Bearer " + userToken))
        .andExpect(status().isForbidden());

    String inventoryToken =
        jwtService.createAccessToken(
            "test-inventory",
            "inventory-manager",
            companyId,
            List.of("INVENTORY"),
            List.of("inventory.manage"),
            false);
    mockMvc
        .perform(
            get("/api/inventory/items")
                .header("Authorization", "Bearer " + inventoryToken))
        .andExpect(status().isOk());
  }

  private String registerPayload() {
    return """
        {
          "username": "%s",
          "email": "%s",
          "password": "%s"
        }
        """
        .formatted(username, email, password);
  }

  private String singleCompanyId() throws Exception {
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement("SELECT id FROM security.company LIMIT 1");
        ResultSet rs = stmt.executeQuery()) {
      if (rs.next()) return rs.getString(1);
    }
    throw new IllegalStateException("Aucune entreprise de test n'est disponible.");
  }

  private String existingUserId() throws Exception {
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt =
            conn.prepareStatement(
                "SELECT id FROM security.app_user WHERE company_id IS NOT NULL LIMIT 1");
        ResultSet rs = stmt.executeQuery()) {
      if (rs.next()) return rs.getString(1);
    }
    throw new IllegalStateException("Aucun utilisateur d'entreprise n'est disponible.");
  }

  private void cleanup() throws Exception {
    try (Connection conn = DatabaseConnection.getConnection()) {
      try (PreparedStatement stmt =
          conn.prepareStatement(
              "DELETE FROM security.user_role WHERE user_id IN "
                  + "(SELECT id FROM security.app_user WHERE username LIKE 'reg-test-%')")) {
        stmt.executeUpdate();
      }
      try (PreparedStatement stmt =
          conn.prepareStatement(
              "DELETE FROM security.app_user WHERE username LIKE 'reg-test-%'")) {
        stmt.executeUpdate();
      }
    }
  }
}
