package sssm.dev.sssm_backend.a11_inventory;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.Filter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
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

/**
 * Routes du module operation (famille générique, create only). L'operation_type n'est pas
 * déduit de la route (dette préexistant) : il doit figurer dans le corps, sinon 500.
 */
@SpringBootTest
class OperationApiTests {

  @Autowired private WebApplicationContext context;
  @Autowired private JwtService jwtService;

  private MockMvc mockMvc;
  private String token;
  private String companyId;

  @BeforeEach
  void setUp() throws Exception {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
            .build();
    token = jwtService.createToken("test-user", "phase5-tester", List.of("ADMIN"));

    try (Connection conn = DatabaseConnection.getConnection()) {
      companyId = firstValue(conn, "SELECT id FROM security.company ORDER BY id LIMIT 1");
    }
    Assumptions.assumeTrue(
        companyId != null, "base initialisée avec des données de démonstration requise");
    purge();
  }

  @AfterEach
  void cleanUp() throws Exception {
    purge();
  }

  @Test
  void receiptCreatesOperationWithDefaultStatus() throws Exception {
    String marker = "phase5-op-" + System.nanoTime();
    String body =
        "{\"company_id\":\""
            + companyId
            + "\",\"operation_type\":\"RECEIPT\",\"reference\":\""
            + marker
            + "\",\"notes\":\""
            + marker
            + "\"}";

    MvcResult created =
        mockMvc
            .perform(
                post("/api/inventory/operations/receipt")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(notNullValue()))
            .andExpect(jsonPath("$.operation_type").value("RECEIPT"))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andReturn();
    String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
    assertNotNull(id);

    // preuve SQL
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt =
            conn.prepareStatement(
                "SELECT operation_type, status FROM security.inventory_operation WHERE id = ?")) {
      stmt.setObject(1, java.util.UUID.fromString(id));
      try (ResultSet rs = stmt.executeQuery()) {
        assertTrue(rs.next(), "l'opération créée doit exister en base");
        assertEquals("RECEIPT", rs.getString("operation_type"));
        assertEquals("PENDING", rs.getString("status"));
      }
    }
  }

  @Test
  void issueAndTransferRoutesCreateRows() throws Exception {
    String marker = "phase5-op2-" + System.nanoTime();

    mockMvc
        .perform(
            post("/api/inventory/operations/issue")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"company_id\":\""
                        + companyId
                        + "\",\"operation_type\":\"ISSUE\",\"notes\":\""
                        + marker
                        + "-issue\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.operation_type").value("ISSUE"));

    mockMvc
        .perform(
            post("/api/inventory/operations/transfer")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"company_id\":\""
                        + companyId
                        + "\",\"operation_type\":\"TRANSFER\",\"notes\":\""
                        + marker
                        + "-transfer\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.operation_type").value("TRANSFER"));
  }

  @Test
  void emptyBodyReturnsSoftErrorIn200() throws Exception {
    mockMvc
        .perform(
            post("/api/inventory/operations/receipt")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.errors.message").value("Request body is required"))
        .andExpect(jsonPath("$.errors.code").value("BAD_REQUEST"));
  }

  @Test
  void missingOperationTypeReturns500Envelope() throws Exception {
    // dette connue : la route n'injecte pas l'operation_type (NOT NULL sans défaut) → 500
    mockMvc
        .perform(
            post("/api/inventory/operations/receipt")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"company_id\":\"" + companyId + "\"}"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.errors.code").value("23502"));
  }

  private static void purge() throws Exception {
    execute("DELETE FROM security.inventory_operation WHERE notes LIKE 'phase5-op%'");
  }

  private static String firstValue(Connection conn, String sql) throws Exception {
    try (PreparedStatement stmt = conn.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()) {
      return rs.next() ? rs.getString(1) : null;
    }
  }

  private static void execute(String sql) throws Exception {
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.executeUpdate();
    }
  }
}
