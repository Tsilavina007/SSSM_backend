package sssm.dev.sssm_backend.a11_inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import java.util.Map;
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

/** Règles métier des mouvements de stock : mise à jour de l'état et de l'entrepôt de l'article. */
@SpringBootTest
class StockMovementBusinessRulesTests {

  @Autowired private WebApplicationContext context;
  @Autowired private JwtService jwtService;

  private MockMvc mockMvc;
  private String token;
  private String companyId;
  private String itemTypeId;
  private String warehouseId;
  private String secondWarehouseId;

  @BeforeEach
  void setUp() throws Exception {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
            .build();
    token = jwtService.createToken("test-user", "phase5-state-tester", List.of("ADMIN"));

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
      itemTypeId = insertItemType(conn, companyId);
    }
  }

  @AfterEach
  void cleanUp() throws Exception {
    purge();
  }

  @Test
  void inMovementMarksItemAvailableInWarehouse() throws Exception {
    String id = createItem("AVAILABLE", null);

    createMovement(id, "IN", warehouseId);

    Map<String, String> state = itemState(id);
    assertEquals("AVAILABLE", state.get("status"));
    assertEquals(warehouseId, state.get("current_warehouse_id"));
  }

  @Test
  void outMovementMarksItemAssignedAndClearsWarehouse() throws Exception {
    String id = createItem("AVAILABLE", warehouseId);

    createMovement(id, "OUT", warehouseId);

    Map<String, String> state = itemState(id);
    assertEquals("ASSIGNED", state.get("status"));
    assertNull(state.get("current_warehouse_id"));
  }

  @Test
  void outMovementRejectsUnavailableItem() throws Exception {
    String id = createItem("ASSIGNED", null);

    mockMvc
        .perform(
            post("/api/inventory/movements")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(movementBody(id, "OUT", warehouseId)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.message").value("Article non disponible"))
        .andExpect(jsonPath("$.errors.code").value("CLIENT_ERROR"));

    assertEquals("ASSIGNED", itemState(id).get("status"));
  }

  @Test
  void transferMovementMovesItemToDestinationWarehouse() throws Exception {
    Assumptions.assumeTrue(
        secondWarehouseId != null, "un second entrepôt est requis pour tester le transfert");
    String id = createItem("AVAILABLE", warehouseId);

    createMovement(id, "TRANSFER", secondWarehouseId);

    assertEquals(secondWarehouseId, itemState(id).get("current_warehouse_id"));
  }

  @Test
  void returnMovementRestoresAvailabilityAndWarehouse() throws Exception {
    String id = createItem("AVAILABLE", warehouseId);
    createMovement(id, "OUT", warehouseId);
    assertEquals("ASSIGNED", itemState(id).get("status"));

    createMovement(id, "RETURN", warehouseId);

    Map<String, String> state = itemState(id);
    assertEquals("AVAILABLE", state.get("status"));
    assertEquals(warehouseId, state.get("current_warehouse_id"));
  }

  @Test
  void destructionMarksItemRetiredAndClearsWarehouse() throws Exception {
    String id = createItem("AVAILABLE", warehouseId);

    createMovement(id, "DESTRUCTION", warehouseId);

    Map<String, String> state = itemState(id);
    assertEquals("RETIRED", state.get("status"));
    assertNull(state.get("current_warehouse_id"));
  }

  @Test
  void adjustmentDoesNotChangeItemState() throws Exception {
    String id = createItem("DAMAGED", warehouseId);

    createMovement(id, "ADJUSTMENT", warehouseId);

    Map<String, String> state = itemState(id);
    assertEquals("DAMAGED", state.get("status"));
    assertEquals(warehouseId, state.get("current_warehouse_id"));
  }

  private String createItem(String status, String warehouse) throws Exception {
    String marker = "phase5-state-" + System.nanoTime();
    String body =
        "{\"company_id\":\""
            + companyId
            + "\",\"item_type_id\":\""
            + itemTypeId
            + "\",\"description\":\""
            + marker
            + "\",\"status\":\""
            + status
            + "\""
            + (warehouse == null ? "" : ",\"current_warehouse_id\":\"" + warehouse + "\"")
            + "}";

    MvcResult created =
        mockMvc
            .perform(
                post("/api/inventory/items")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isCreated())
            .andReturn();
    String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
    assertNotNull(id);
    return id;
  }

  private void createMovement(String itemId, String type, String warehouse) throws Exception {
    mockMvc
        .perform(
            post("/api/inventory/movements")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(movementBody(itemId, type, warehouse)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.notNullValue()));
  }

  private static String movementBody(String itemId, String type, String warehouse) {
    String adjustmentDestination =
        "ADJUSTMENT".equals(type) || "TRANSFER".equals(type)
            ? ",\"destination_warehouse_id\":\"" + warehouse + "\""
            : "";
    return "{\"warehouse_id\":\""
        + warehouse
        + "\",\"item_id\":\""
        + itemId
        + "\",\"movement_type\":\""
        + type
        + "\""
        + adjustmentDestination
        + "}";
  }

  private static String insertItemType(Connection conn, String companyId) throws Exception {
    String code = "phase5-state-type";
    try (PreparedStatement stmt =
        conn.prepareStatement(
            "INSERT INTO security.item_type (company_id, code, name) VALUES (?, ?, ?) RETURNING id")) {
      stmt.setObject(1, java.util.UUID.fromString(companyId));
      stmt.setString(2, code);
      stmt.setString(3, code);
      try (ResultSet rs = stmt.executeQuery()) {
        rs.next();
        return rs.getString(1);
      }
    }
  }

  private static Map<String, String> itemState(String id) throws Exception {
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt =
            conn.prepareStatement(
                "SELECT status, current_warehouse_id FROM security.item WHERE id = ?")) {
      stmt.setObject(1, UUID.fromString(id));
      try (ResultSet rs = stmt.executeQuery()) {
        assertTrue(rs.next(), "l'article doit exister");
        Map<String, String> state = new java.util.HashMap<>();
        state.put("status", rs.getString("status"));
        state.put("current_warehouse_id", rs.getString("current_warehouse_id"));
        return state;
      }
    }
  }

  private static void purge() throws Exception {
    execute(
        "DELETE FROM security.stock_movement WHERE item_id IN "
            + "(SELECT id FROM security.item WHERE description LIKE 'phase5-state-%')");
    execute(
        "DELETE FROM security.item_assignment WHERE item_id IN "
            + "(SELECT id FROM security.item WHERE description LIKE 'phase5-state-%')");
    execute(
        "DELETE FROM security.stock_balance WHERE item_type_id IN "
            + "(SELECT id FROM security.item_type WHERE code = 'phase5-state-type')");
    execute("DELETE FROM security.item WHERE description LIKE 'phase5-state-%'");
    execute("DELETE FROM security.item_type WHERE code = 'phase5-state-type'");
  }

  private static List<String> values(Connection conn, String sql) throws Exception {
    java.util.List<String> result = new java.util.ArrayList<>();
    try (PreparedStatement stmt = conn.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        result.add(rs.getString(1));
      }
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
