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
import java.math.BigDecimal;
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

/**
 * Routes du module operation : le type est imposé par la route et chaque opération porte au moins
 * une ligne, un mouvement et une mise à jour de solde atomiques.
 */
@SpringBootTest
class OperationApiTests {

  @Autowired private WebApplicationContext context;
  @Autowired private JwtService jwtService;

  private MockMvc mockMvc;
  private String token;
  private String companyId;
  private String warehouseId;
  private String secondWarehouseId;
  private String itemTypeId;

  @BeforeEach
  void setUp() throws Exception {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
            .build();
    token = jwtService.createToken("test-user", "phase5-tester", List.of("ADMIN"));

    try (Connection conn = DatabaseConnection.getConnection()) {
      companyId = firstValue(conn, "SELECT id FROM security.company ORDER BY id LIMIT 1");
      List<String> warehouseIds =
          values(conn, "SELECT id FROM security.warehouse ORDER BY id LIMIT 2");
      warehouseId = warehouseIds.isEmpty() ? null : warehouseIds.get(0);
      secondWarehouseId = warehouseIds.size() < 2 ? null : warehouseIds.get(1);
    }
    Assumptions.assumeTrue(
        companyId != null && warehouseId != null,
        "base initialisée avec des données de démonstration requise");
    purge();
    try (Connection conn = DatabaseConnection.getConnection()) {
      itemTypeId = insertItemType(conn);
    }
  }

  @AfterEach
  void cleanUp() throws Exception {
    purge();
  }

  @Test
  void receiptCreatesCompletedOperationWithLineAndMovement() throws Exception {
    String marker = "phase5-op-" + System.nanoTime();
    String itemId = insertItem("phase5-op-item-" + System.nanoTime(), "AVAILABLE", null);
    String body =
        "{\"company_id\":\""
            + companyId
            + "\",\"reference\":\""
            + marker
            + "\",\"destination_warehouse_id\":\""
            + warehouseId
            + "\",\"lines\":[{\"item_id\":\""
            + itemId
            + "\",\"quantity\":3}]}";

    MvcResult created =
        mockMvc
            .perform(
                post("/api/inventory/operations/receipt")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(notNullValue()))
            .andExpect(jsonPath("$.operation_type").value("RECEIPT"))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.completed_at").value(notNullValue()))
            .andExpect(jsonPath("$.lines.length()").value(1))
            .andReturn();
    String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
    assertNotNull(id);

    try (Connection conn = DatabaseConnection.getConnection()) {
      try (PreparedStatement stmt =
          conn.prepareStatement(
              "SELECT status, operation_type FROM security.inventory_operation WHERE id = ?")) {
        stmt.setObject(1, UUID.fromString(id));
        try (ResultSet rs = stmt.executeQuery()) {
          assertTrue(rs.next(), "l'opération créée doit exister en base");
          assertEquals("COMPLETED", rs.getString("status"));
          assertEquals("RECEIPT", rs.getString("operation_type"));
        }
      }
      try (PreparedStatement stmt =
          conn.prepareStatement(
              "SELECT m.quantity, m.destination_warehouse_id, l.id AS line_id "
                  + "FROM security.stock_movement m "
                  + "JOIN security.inventory_operation_item l ON l.movement_id = m.id "
                  + "WHERE m.operation_id = ?")) {
        stmt.setObject(1, UUID.fromString(id));
        try (ResultSet rs = stmt.executeQuery()) {
          assertTrue(rs.next(), "un mouvement et une ligne doivent être créés");
          assertEquals(0, new BigDecimal("3").compareTo(rs.getBigDecimal("quantity")));
          assertEquals(warehouseId, rs.getString("destination_warehouse_id"));
          assertNotNull(rs.getString("line_id"));
        }
      }
      assertEquals(0, new BigDecimal("3").compareTo(balance(itemTypeId, warehouseId)));
      assertEquals("AVAILABLE", itemState(itemId).get("status"));
      assertEquals(warehouseId, itemState(itemId).get("current_warehouse_id"));
    }
  }

  @Test
  void issueAndTransferRoutesCreateRows() throws Exception {
    Assumptions.assumeTrue(
        secondWarehouseId != null, "un second entrepôt est requis pour tester le transfert");
    String issueMarker = "phase5-op-issue-" + System.nanoTime();
    String issueItem = insertItem("phase5-op-item-" + System.nanoTime(), "AVAILABLE", warehouseId);

    mockMvc
        .perform(
            post("/api/inventory/operations/issue")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"company_id\":\""
                        + companyId
                        + "\",\"reference\":\""
                        + issueMarker
                        + "\",\"source_warehouse_id\":\""
                        + warehouseId
                        + "\",\"lines\":[{\"item_id\":\""
                        + issueItem
                        + "\"}]}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.operation_type").value("ISSUE"))
        .andExpect(jsonPath("$.lines.length()").value(1));
    assertEquals("ASSIGNED", itemState(issueItem).get("status"));

    String transferMarker = "phase5-op-transfer-" + System.nanoTime();
    String transferItem =
        insertItem("phase5-op-item-" + System.nanoTime(), "AVAILABLE", warehouseId);

    mockMvc
        .perform(
            post("/api/inventory/operations/transfer")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"company_id\":\""
                        + companyId
                        + "\",\"reference\":\""
                        + transferMarker
                        + "\",\"source_warehouse_id\":\""
                        + warehouseId
                        + "\",\"destination_warehouse_id\":\""
                        + secondWarehouseId
                        + "\",\"lines\":[{\"item_id\":\""
                        + transferItem
                        + "\"}]}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.operation_type").value("TRANSFER"))
        .andExpect(jsonPath("$.lines.length()").value(1));
    assertEquals(secondWarehouseId, itemState(transferItem).get("current_warehouse_id"));
  }

  @Test
  void emptyBodyReturnsBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/inventory/operations/receipt")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.message").value("Entreprise obligatoire"));
  }

  @Test
  void operationWithoutLinesReturnsBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/inventory/operations/receipt")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"company_id\":\"" + companyId + "\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.message").value("Ligne d'article obligatoire"));
  }

  private String insertItemType(Connection conn) throws Exception {
    String code = "phase5-op-type-" + System.nanoTime();
    try (PreparedStatement stmt =
        conn.prepareStatement(
            "INSERT INTO security.item_type (company_id, code, name) VALUES (?, ?, ?) RETURNING id")) {
      stmt.setObject(1, UUID.fromString(companyId));
      stmt.setString(2, code);
      stmt.setString(3, code);
      try (ResultSet rs = stmt.executeQuery()) {
        rs.next();
        return rs.getString(1);
      }
    }
  }

  private String insertItem(String marker, String status, String warehouse) throws Exception {
    try (Connection conn = DatabaseConnection.getConnection()) {
      String id;
      try (PreparedStatement stmt =
          conn.prepareStatement(
              "INSERT INTO security.item "
                  + "(company_id, item_type_id, qr_code, status, current_warehouse_id) "
                  + "VALUES (?, ?, ?, CAST(? AS security.item_status), ?) RETURNING id")) {
        stmt.setObject(1, UUID.fromString(companyId));
        stmt.setObject(2, UUID.fromString(itemTypeId));
        stmt.setString(3, marker);
        stmt.setString(4, status);
        stmt.setObject(5, warehouse == null ? null : UUID.fromString(warehouse));
        try (ResultSet rs = stmt.executeQuery()) {
          rs.next();
          id = rs.getString(1);
        }
      }
      if (warehouse != null) {
        try (PreparedStatement stmt =
            conn.prepareStatement(
                "INSERT INTO security.stock_balance "
                    + "(company_id, warehouse_id, item_type_id, quantity) VALUES (?, ?, ?, 1) "
                    + "ON CONFLICT (company_id, warehouse_id, item_type_id) DO UPDATE SET "
                    + "quantity = security.stock_balance.quantity + 1, version = security.stock_balance.version + 1")) {
          stmt.setObject(1, UUID.fromString(companyId));
          stmt.setObject(2, UUID.fromString(warehouse));
          stmt.setObject(3, UUID.fromString(itemTypeId));
          stmt.executeUpdate();
        }
      }
      return id;
    }
  }

  private static BigDecimal balance(String itemTypeId, String warehouseId) throws Exception {
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt =
            conn.prepareStatement(
                "SELECT quantity FROM security.stock_balance "
                    + "WHERE item_type_id=? AND warehouse_id=?")) {
      stmt.setObject(1, UUID.fromString(itemTypeId));
      stmt.setObject(2, UUID.fromString(warehouseId));
      try (ResultSet rs = stmt.executeQuery()) {
        return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
      }
    }
  }

  private static java.util.Map<String, String> itemState(String id) throws Exception {
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt =
            conn.prepareStatement(
                "SELECT status, current_warehouse_id FROM security.item WHERE id = ?")) {
      stmt.setObject(1, UUID.fromString(id));
      try (ResultSet rs = stmt.executeQuery()) {
        assertTrue(rs.next(), "l'article doit exister");
        java.util.Map<String, String> state = new java.util.HashMap<>();
        state.put("status", rs.getString("status"));
        state.put("current_warehouse_id", rs.getString("current_warehouse_id"));
        return state;
      }
    }
  }

  private static void purge() throws Exception {
    execute(
        "DELETE FROM security.inventory_operation_item WHERE operation_id IN "
            + "(SELECT id FROM security.inventory_operation WHERE notes LIKE 'phase5-op%' "
            + "OR reference LIKE 'phase5-op%')");
    execute(
        "DELETE FROM security.stock_movement WHERE operation_id IN "
            + "(SELECT id FROM security.inventory_operation WHERE notes LIKE 'phase5-op%' "
            + "OR reference LIKE 'phase5-op%')");
    execute(
        "DELETE FROM security.inventory_operation WHERE notes LIKE 'phase5-op%' "
            + "OR reference LIKE 'phase5-op%'");
    execute(
        "DELETE FROM security.stock_balance WHERE item_type_id IN "
            + "(SELECT id FROM security.item_type WHERE code LIKE 'phase5-op-type-%')");
    execute(
        "DELETE FROM security.item WHERE item_type_id IN "
            + "(SELECT id FROM security.item_type WHERE code LIKE 'phase5-op-type-%')");
    execute("DELETE FROM security.item_type WHERE code LIKE 'phase5-op-type-%'");
  }

  private static List<String> values(Connection conn, String sql) throws Exception {
    java.util.List<String> result = new java.util.ArrayList<>();
    try (PreparedStatement stmt = conn.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) result.add(rs.getString(1));
    }
    return result;
  }

  private static String firstValue(Connection conn, String sql) throws Exception {
    List<String> found = values(conn, sql);
    return found.isEmpty() ? null : found.get(0);
  }

  private static void execute(String sql) throws Exception {
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.executeUpdate();
    }
  }
}
