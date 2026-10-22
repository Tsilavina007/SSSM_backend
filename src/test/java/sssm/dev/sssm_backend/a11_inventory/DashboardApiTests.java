package sssm.dev.sssm_backend.a11_inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.Filter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a03_authentication.auth.service.JwtService;

/**
 * Routes du module dashboard : synthèse et alertes de stock bas (lecture seule, réponses en
 * tableaux — un agrégat par entreprise pour la synthèse).
 */
@SpringBootTest
class DashboardApiTests {

  @Autowired private WebApplicationContext context;
  @Autowired private JwtService jwtService;

  private MockMvc mockMvc;
  private String token;

  @BeforeEach
  void setUp() throws Exception {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
            .build();
    token = jwtService.createToken("test-user", "phase5-tester", List.of("ADMIN"));
    purge();
  }

  @AfterEach
  void cleanUp() throws Exception {
    purge();
  }

  @Test
  void summaryReturnsArrayOfCounters() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get("/api/inventory/dashboard/summary")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();

    List<?> rows = JsonPath.read(result.getResponse().getContentAsString(), "$");
    if (!rows.isEmpty()) {
      Map<?, ?> first = (Map<?, ?>) rows.get(0);
      assertNotNull(first.get("items"), "compteur d'articles attendu");
      assertNotNull(first.get("warehouses"), "compteur d'entrepôts attendu");
      assertNotNull(first.get("operations"), "compteur d'opérations attendu");
    }
  }

  @Test
  void lowStockReturnsArray() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get("/api/inventory/alerts/low-stock").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();

    List<?> rows = JsonPath.read(result.getResponse().getContentAsString(), "$");
    if (!rows.isEmpty()) {
      Map<?, ?> first = (Map<?, ?>) rows.get(0);
      assertNotNull(first.get("id"), "identifiant de l'article attendu");
      assertNotNull(first.get("stock_quantity"), "quantité en stock attendue");
    }
  }

  @Test
  void lowStockFlagsOnlyTypesBelowThreshold() throws Exception {
    String lowCode = "phase5-lowstock-low-" + System.nanoTime();
    String okCode = "phase5-lowstock-ok-" + System.nanoTime();
    String lowId = insertItemType(lowCode, new BigDecimal("2"), new BigDecimal("5"));
    String okId = insertItemType(okCode, new BigDecimal("10"), new BigDecimal("5"));

    MvcResult result =
        mockMvc
            .perform(
                get("/api/inventory/alerts/low-stock").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();

    List<Map<String, Object>> rows =
        JsonPath.read(result.getResponse().getContentAsString(), "$");
    Map<String, Object> lowRow =
        rows.stream().filter(row -> lowId.equals(row.get("id"))).findFirst().orElse(null);
    assertNotNull(lowRow, "le type sous le seuil doit être signalé");
    assertTrue(
        new BigDecimal(String.valueOf(lowRow.get("stock_quantity")))
                .compareTo(new BigDecimal(String.valueOf(lowRow.get("minimum_stock"))))
            < 0,
        "stock_quantity doit être inférieur à minimum_stock");
    assertFalse(
        rows.stream().anyMatch(row -> okId.equals(row.get("id"))),
        "le type au-dessus du seuil ne doit pas être signalé");
  }

  private String insertItemType(String code, BigDecimal quantity, BigDecimal minimum)
      throws Exception {
    String id;
    try (Connection conn = DatabaseConnection.getConnection()) {
      String companyId;
      String warehouseId;
      try (PreparedStatement stmt =
          conn.prepareStatement(
              "SELECT (SELECT id FROM security.company ORDER BY id LIMIT 1) AS company_id, "
                  + "(SELECT id FROM security.warehouse ORDER BY id LIMIT 1) AS warehouse_id");
          ResultSet rs = stmt.executeQuery()) {
        assertTrue(rs.next(), "des données de démonstration sont requises");
        companyId = rs.getString("company_id");
        warehouseId = rs.getString("warehouse_id");
      }
      try (PreparedStatement stmt =
          conn.prepareStatement(
              "INSERT INTO security.item_type "
                  + "(company_id, code, name, quantity_total, minimum_stock) "
                  + "VALUES (?, ?, ?, 0, ?) RETURNING id")) {
        stmt.setObject(1, java.util.UUID.fromString(companyId));
        stmt.setString(2, code);
        stmt.setString(3, code);
        stmt.setBigDecimal(4, minimum);
        try (ResultSet rs = stmt.executeQuery()) {
          assertTrue(rs.next());
          id = rs.getString(1);
        }
      }
      try (PreparedStatement stmt =
          conn.prepareStatement(
              "INSERT INTO security.stock_balance "
                  + "(company_id, warehouse_id, item_type_id, quantity) VALUES (?, ?, ?, ?)")) {
        stmt.setObject(1, java.util.UUID.fromString(companyId));
        stmt.setObject(2, java.util.UUID.fromString(warehouseId));
        stmt.setObject(3, java.util.UUID.fromString(id));
        stmt.setBigDecimal(4, quantity);
        stmt.executeUpdate();
      }
    }
    return id;
  }

  private static void purge() throws Exception {
    try (Connection conn = DatabaseConnection.getConnection()) {
      try (PreparedStatement stmt =
          conn.prepareStatement(
              "DELETE FROM security.stock_balance WHERE item_type_id IN "
                  + "(SELECT id FROM security.item_type WHERE code LIKE 'phase5-lowstock-%')")) {
        stmt.executeUpdate();
      }
      try (PreparedStatement stmt =
          conn.prepareStatement(
              "DELETE FROM security.item_type WHERE code LIKE 'phase5-lowstock-%'")) {
        stmt.executeUpdate();
      }
    }
  }
}
