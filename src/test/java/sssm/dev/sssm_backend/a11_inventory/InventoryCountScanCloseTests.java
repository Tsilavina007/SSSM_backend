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
import java.util.UUID;
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

@SpringBootTest
class InventoryCountScanCloseTests {

  @Autowired private WebApplicationContext context;
  @Autowired private JwtService jwtService;

  private MockMvc mockMvc;
  private String token;
  private String companyId;
  private String warehouseId;
  private String itemId;

  @BeforeEach
  void setUp() throws Exception {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
            .build();
    token = jwtService.createToken("test-user", "phase3-tester", List.of("ADMIN"));

    try (Connection conn = DatabaseConnection.getConnection()) {
      companyId = firstId(conn, "SELECT id FROM security.company ORDER BY id LIMIT 1");
      warehouseId = firstId(conn, "SELECT id FROM security.warehouse ORDER BY id LIMIT 1");
      itemId = firstId(conn, "SELECT id FROM security.item ORDER BY id LIMIT 1");
    }
    Assumptions.assumeTrue(
        companyId != null && warehouseId != null && itemId != null,
        "base initialisée avec des données de démonstration requise");
    purge();
  }

  @AfterEach
  void cleanUp() throws Exception {
    purge();
  }

  @Test
  void closeWithoutBodyCompletesCount() throws Exception {
    String countId = createCount("phase3-close");

    mockMvc
        .perform(
            post("/api/inventory/inventory-counts/" + countId + "/close")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(countId))
        .andExpect(jsonPath("$.status").value("COMPLETED"));

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt =
            conn.prepareStatement("SELECT status FROM security.inventory_count WHERE id = ?")) {
      stmt.setObject(1, UUID.fromString(countId));
      try (ResultSet rs = stmt.executeQuery()) {
        assertTrue(rs.next(), "le décompte doit exister");
        assertEquals("COMPLETED", rs.getString("status"), "status doit passer à COMPLETED");
      }
    }
  }

  @Test
  void scanInsertsRowInScanTable() throws Exception {
    String countId = createCount("phase3-scan");
    String marker = "phase3-scanok-" + System.nanoTime();
    String body = "{\"item_id\":\"" + itemId + "\",\"notes\":\"" + marker + "\"}";

    mockMvc
        .perform(
            post("/api/inventory/inventory-counts/" + countId + "/scans")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(notNullValue()))
        .andExpect(jsonPath("$.inventory_count_id").value(countId))
        .andExpect(jsonPath("$.item_id").value(itemId))
        .andExpect(jsonPath("$.qr_code").value(notNullValue()))
        .andExpect(jsonPath("$.scanned_at").value(notNullValue()));

    try (Connection conn = DatabaseConnection.getConnection()) {
      try (PreparedStatement stmt =
          conn.prepareStatement(
              "SELECT inventory_count_id, scanned_at FROM security.inventory_count_scan"
                  + " WHERE notes = ?")) {
        stmt.setString(1, marker);
        try (ResultSet rs = stmt.executeQuery()) {
          assertTrue(rs.next(), "la ligne de scan doit être créée");
          assertEquals(countId, rs.getString("inventory_count_id"));
          assertNotNull(
              rs.getTimestamp("scanned_at"),
              "scanned_at doit être renseigné par le défaut PostgreSQL");
        }
      }
      try (PreparedStatement stmt =
          conn.prepareStatement("SELECT count(*) FROM security.inventory_count WHERE notes = ?")) {
        stmt.setString(1, marker);
        try (ResultSet rs = stmt.executeQuery()) {
          rs.next();
          assertEquals(
              0,
              rs.getInt(1),
              "le scan ne doit plus insérer dans la table inventory_count");
        }
      }
    }
  }

  @Test
  void scanWithoutItemIdReturnsBadRequest() throws Exception {
    String countId = createCount("phase3-noitem");

    mockMvc
        .perform(
            post("/api/inventory/inventory-counts/" + countId + "/scans")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"notes\":\"sans item\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void scanWithUnknownFieldReturnsBadRequest() throws Exception {
    String countId = createCount("phase3-unknown");
    String body = "{\"item_id\":\"" + itemId + "\",\"bogus\":\"x\"}";

    mockMvc
        .perform(
            post("/api/inventory/inventory-counts/" + countId + "/scans")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void scanOnMissingCountReturnsNotFound() throws Exception {
    String body = "{\"item_id\":\"" + itemId + "\"}";

    mockMvc
        .perform(
            post(
                    "/api/inventory/inventory-counts/"
                        + UUID.randomUUID()
                        + "/scans")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isNotFound());
  }

  @Test
  void duplicateScanReturnsBadRequest() throws Exception {
    String countId = createCount("phase3-dup");
    String body = "{\"item_id\":\"" + itemId + "\"}";

    mockMvc
        .perform(
            post("/api/inventory/inventory-counts/" + countId + "/scans")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/inventory/inventory-counts/" + countId + "/scans")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());
  }

  private String createCount(String markerPrefix) throws Exception {
    String marker = markerPrefix + "-" + System.nanoTime();
    String reference = marker;
    String body =
        "{\"company_id\":\""
            + companyId
            + "\",\"warehouse_id\":\""
            + warehouseId
            + "\",\"reference\":\""
            + reference
            + "\",\"notes\":\""
            + marker
            + "\"}";

    MvcResult result =
        mockMvc
            .perform(
                post("/api/inventory/inventory-counts")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(notNullValue()))
            .andReturn();
    String id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    assertNotNull(id);
    return id;
  }

  private static void purge() throws Exception {
    execute(
        "DELETE FROM security.inventory_count_scan WHERE inventory_count_id IN "
            + "(SELECT id FROM security.inventory_count WHERE notes LIKE 'phase3-%')");
    execute("DELETE FROM security.inventory_count WHERE notes LIKE 'phase3-%'");
  }

  private static String firstId(Connection conn, String sql) throws Exception {
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
