package sssm.dev.sssm_backend.a11_inventory;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.Filter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
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
 * Preuves du Lot 3 : solde calculé, journal immuable, opérations atomiques et comptage réconcilié.
 *
 * <p>Les données sont isolées par un type d'article et des marqueurs {@code phase3-lot3-*},
 * nettoyés avant et après chaque test.
 */
@SpringBootTest
class Lot3StockModelTests {

  private static final String TYPE_CODE = "phase3-lot3-type";
  private static final String MARKER = "phase3-lot3-";

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
    token = jwtService.createToken("test-user", "lot3-tester", List.of("ADMIN"));

    try (Connection conn = DatabaseConnection.getConnection()) {
      companyId = firstId(conn, "SELECT id FROM security.company ORDER BY id LIMIT 1");
      List<String> warehouses =
          values(conn, "SELECT id FROM security.warehouse ORDER BY id LIMIT 2");
      warehouseId = warehouses.isEmpty() ? null : warehouses.get(0);
      secondWarehouseId = warehouses.size() < 2 ? null : warehouses.get(1);
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
  void balanceFollowsMovementsAndIsExposedByApi() throws Exception {
    String marker = MARKER + "balance-" + System.nanoTime();
    String itemId = insertItem(marker + "-item", null);

    mockMvc
        .perform(
            post("/api/inventory/movements")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"warehouse_id\":\""
                        + warehouseId
                        + "\",\"item_id\":\""
                        + itemId
                        + "\",\"movement_type\":\"IN\",\"quantity\":2,\"notes\":\""
                        + marker
                        + "\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.movement_type").value("IN"))
        .andExpect(jsonPath("$.quantity").isNumber())
        .andExpect(jsonPath("$.destination_warehouse_id").value(warehouseId));

    assertEquals(0, amount("2").compareTo(balance(warehouseId)));
    assertEquals("AVAILABLE", itemState(itemId).get("status"));
    assertEquals(warehouseId, itemState(itemId).get("current_warehouse_id"));

    MvcResult afterReceipt = balanceApi();
    assertEquals(1, ((List<?>) JsonPath.read(body(afterReceipt), "$")).size());
    assertEquals(
        0,
        amount("2")
            .compareTo(
                new BigDecimal(
                    String.valueOf((Object) JsonPath.read(body(afterReceipt), "$[0].quantity")))));

    mockMvc
        .perform(
            post("/api/inventory/movements")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"warehouse_id\":\""
                        + warehouseId
                        + "\",\"item_id\":\""
                        + itemId
                        + "\",\"movement_type\":\"OUT\",\"quantity\":1,\"notes\":\""
                        + marker
                        + "-out\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.movement_type").value("OUT"));

    assertEquals(0, amount("1").compareTo(balance(warehouseId)));
    assertEquals("ASSIGNED", itemState(itemId).get("status"));
    assertEquals(null, itemState(itemId).get("current_warehouse_id"));

    MvcResult afterIssue = balanceApi();
    assertEquals(
        0,
        amount("1")
            .compareTo(
                new BigDecimal(
                    String.valueOf((Object) JsonPath.read(body(afterIssue), "$[0].quantity")))));
  }

  @Test
  void movementRecordsAuthenticatedActor() throws Exception {
    String marker = MARKER + "actor-" + System.nanoTime();
    String actorId = UUID.randomUUID().toString();
    String actorToken = jwtService.createToken(actorId, "lot3-actor", List.of("ADMIN"));
    String itemId = insertItem(marker + "-item", null);

    mockMvc
        .perform(
            post("/api/inventory/movements")
                .header("Authorization", "Bearer " + actorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"warehouse_id\":\""
                        + warehouseId
                        + "\",\"item_id\":\""
                        + itemId
                        + "\",\"movement_type\":\"IN\",\"notes\":\""
                        + marker
                        + "\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.performed_by").value(actorId));

    assertEquals(
        1,
        count(
            "SELECT count(*) FROM security.stock_movement "
                + "WHERE notes = ? AND performed_by = ?",
            marker,
            UUID.fromString(actorId)));
  }

  @Test
  void insufficientStockRejectsMovementAndLeavesStateUnchanged() throws Exception {
    String marker = MARKER + "negative-" + System.nanoTime();
    String itemId = insertItem(marker + "-item", warehouseId);

    mockMvc
        .perform(
            post("/api/inventory/movements")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"warehouse_id\":\""
                        + warehouseId
                        + "\",\"item_id\":\""
                        + itemId
                        + "\",\"movement_type\":\"OUT\",\"quantity\":2,\"notes\":\""
                        + marker
                        + "\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errors.code").value("INSUFFICIENT_STOCK"));

    assertEquals(0, amount("1").compareTo(balance(warehouseId)));
    assertEquals("AVAILABLE", itemState(itemId).get("status"));
    assertEquals(warehouseId, itemState(itemId).get("current_warehouse_id"));
    assertEquals(0, count("SELECT count(*) FROM security.stock_movement WHERE notes = ?", marker));
  }

  @Test
  void movementJournalIsAppendOnly() throws Exception {
    String marker = MARKER + "immutable-" + System.nanoTime();
    String itemId = insertItem(marker + "-item", null);

    MvcResult created =
        mockMvc
            .perform(
                post("/api/inventory/movements")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"warehouse_id\":\""
                            + warehouseId
                            + "\",\"item_id\":\""
                            + itemId
                            + "\",\"notes\":\""
                            + marker
                            + "\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    String movementId = JsonPath.read(body(created), "$.id");

    mockMvc
        .perform(
            patch("/api/inventory/movements/" + movementId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"notes\":\"tentative de réécriture\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errors.code").value("IMMUTABLE_RESOURCE"));

    mockMvc
        .perform(
            delete("/api/inventory/movements/" + movementId)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errors.code").value("IMMUTABLE_RESOURCE"));

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt =
            conn.prepareStatement(
                "SELECT notes FROM security.stock_movement WHERE id = ?")) {
      stmt.setObject(1, UUID.fromString(movementId));
      try (ResultSet rs = stmt.executeQuery()) {
        assertTrue(rs.next(), "le mouvement doit rester présent après PATCH/DELETE");
        assertEquals(marker, rs.getString("notes"));
      }
    }
    assertEquals(0, amount("1").compareTo(balance(warehouseId)));
  }

  @Test
  void multiLineOperationRollsBackHeaderLinesMovementsAndBalance() throws Exception {
    String marker = MARKER + "rollback-" + System.nanoTime();
    String itemId = insertItem(marker + "-item", null);
    String missingItem = UUID.randomUUID().toString();

    mockMvc
        .perform(
            post("/api/inventory/operations/receipt")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"company_id\":\""
                        + companyId
                        + "\",\"reference\":\""
                        + marker
                        + "\",\"destination_warehouse_id\":\""
                        + warehouseId
                        + "\",\"notes\":\""
                        + marker
                        + "\",\"lines\":["
                        + "{\"item_id\":\""
                        + itemId
                        + "\",\"quantity\":2},"
                        + "{\"item_id\":\""
                        + missingItem
                        + "\",\"quantity\":1}]}"))
        .andExpect(status().isNotFound());

    assertEquals(0, amount("0").compareTo(balance(warehouseId)));
    assertEquals("AVAILABLE", itemState(itemId).get("status"));
    assertEquals(null, itemState(itemId).get("current_warehouse_id"));
    assertEquals(
        0,
        count(
            "SELECT count(*) FROM security.inventory_operation WHERE notes = ? OR reference = ?",
            marker,
            marker));
    assertEquals(
        0,
        count("SELECT count(*) FROM security.stock_movement WHERE notes = ?", marker));
    assertEquals(
        0,
        count(
            "SELECT count(*) FROM security.inventory_operation_item WHERE item_id = ?",
            UUID.fromString(itemId)));
  }

  @Test
  void countFreezesExpectedQuantityAndCloseReconcilesBalances() throws Exception {
    String marker = MARKER + "count-" + System.nanoTime();
    String lostItem = insertItem(marker + "-lost", warehouseId);
    String countedItem = insertItem(marker + "-counted", warehouseId);
    assertEquals(0, amount("2").compareTo(balance(warehouseId)));

    String countId = createCount(marker);

    MvcResult lostScan =
        mockMvc
            .perform(
                post("/api/inventory/inventory-counts/" + countId + "/scans")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"item_id\":\""
                            + lostItem
                            + "\",\"counted_quantity\":0,\"notes\":\""
                            + marker
                            + "-lost\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.expected_quantity").isNumber())
            .andExpect(jsonPath("$.counted_quantity").isNumber())
            .andExpect(jsonPath("$.difference").isNumber())
            .andExpect(jsonPath("$.discrepancy_type").value("MISSING"))
            .andReturn();
    assertDecimal(lostScan, "$.expected_quantity", "1");
    assertDecimal(lostScan, "$.counted_quantity", "0");
    assertDecimal(lostScan, "$.difference", "-1");

    MvcResult countedScan =
        mockMvc
            .perform(
                post("/api/inventory/inventory-counts/" + countId + "/scans")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"item_id\":\""
                            + countedItem
                            + "\",\"counted_quantity\":2,\"notes\":\""
                            + marker
                            + "-counted\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.discrepancy_type").value("NONE"))
            .andReturn();
    assertDecimal(countedScan, "$.expected_quantity", "1");
    assertDecimal(countedScan, "$.counted_quantity", "2");
    assertDecimal(countedScan, "$.difference", "1");

    mockMvc
        .perform(
            get("/api/inventory/inventory-counts/" + countId + "/results")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));

    mockMvc
        .perform(
            post("/api/inventory/inventory-counts/" + countId + "/close")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.completed_at").value(notNullValue()));

    assertEquals("LOST", itemState(lostItem).get("status"));
    assertEquals(null, itemState(lostItem).get("current_warehouse_id"));
    assertEquals("AVAILABLE", itemState(countedItem).get("status"));
    assertEquals(warehouseId, itemState(countedItem).get("current_warehouse_id"));
    assertNotNull(itemState(lostItem).get("last_inventory_at"));
    assertNotNull(itemState(countedItem).get("last_inventory_at"));
    assertEquals(0, amount("2").compareTo(balance(warehouseId)));
    assertEquals(
        1,
        count(
            "SELECT count(*) FROM security.inventory_count "
                + "WHERE id = ? AND status = 'COMPLETED' AND completed_at IS NOT NULL",
            UUID.fromString(countId)));

    mockMvc
        .perform(
            post("/api/inventory/inventory-counts/" + countId + "/close")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errors.code").value("COUNT_NOT_OPEN"));
  }

  @Test
  void countCloseRollsBackAllScansWhenOneReconciliationFails() throws Exception {
    Assumptions.assumeTrue(secondWarehouseId != null, "deux entrepôts de démonstration requis");
    String marker = MARKER + "count-rollback-" + System.nanoTime();
    String firstItem = insertItem(marker + "-first", warehouseId);
    String secondItem = insertItem(marker + "-second", secondWarehouseId);
    String countId = createCount(marker);

    mockMvc
        .perform(
            post("/api/inventory/inventory-counts/" + countId + "/scans")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"item_id\":\""
                        + firstItem
                        + "\",\"counted_quantity\":0,\"notes\":\""
                        + marker
                        + "-first\"}"))
        .andExpect(status().isOk());
    Thread.sleep(25);
    mockMvc
        .perform(
            post("/api/inventory/inventory-counts/" + countId + "/scans")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"item_id\":\""
                        + secondItem
                        + "\",\"actual_warehouse_id\":\""
                        + secondWarehouseId
                        + "\",\"counted_quantity\":0,\"notes\":\""
                        + marker
                        + "-second\"}"))
        .andExpect(status().isOk());

    // Simule un solde manquant/incohérent pour le second entrepôt : la clôture doit
    // échouer et annuler aussi la réconciliation déjà effectuée sur le premier.
    execute(
        "DELETE FROM security.stock_balance WHERE item_type_id = '"
            + itemTypeId
            + "' AND warehouse_id = '"
            + secondWarehouseId
            + "'");

    mockMvc
        .perform(
            post("/api/inventory/inventory-counts/" + countId + "/close")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errors.code").value("INSUFFICIENT_STOCK"));

    assertEquals(0, amount("1").compareTo(balance(warehouseId)));
    assertEquals("AVAILABLE", itemState(firstItem).get("status"));
    assertEquals(warehouseId, itemState(firstItem).get("current_warehouse_id"));
    assertEquals(null, itemState(firstItem).get("last_inventory_at"));
    assertEquals("AVAILABLE", itemState(secondItem).get("status"));
    assertEquals(secondWarehouseId, itemState(secondItem).get("current_warehouse_id"));
    assertEquals(null, itemState(secondItem).get("last_inventory_at"));
    assertEquals(
        1,
        count(
            "SELECT count(*) FROM security.inventory_count "
                + "WHERE id = ? AND status = 'IN_PROGRESS' AND completed_at IS NULL",
            UUID.fromString(countId)));
  }

  @Test
  void unknownCountAndResultsReturnNotFound() throws Exception {
    String marker = MARKER + "notfound-" + System.nanoTime();
    String countId = createCount(marker);
    String missing = UUID.randomUUID().toString();

    mockMvc
        .perform(
            get("/api/inventory/inventory-counts/" + countId)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(countId));

    mockMvc
        .perform(
            get("/api/inventory/inventory-counts/" + missing)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errors.code").value("NOT_FOUND"));

    mockMvc
        .perform(
            get("/api/inventory/inventory-counts/" + missing + "/results")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errors.code").value("NOT_FOUND"));

    mockMvc
        .perform(
            post("/api/inventory/inventory-counts/" + missing + "/scans")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"item_id\":\"" + UUID.randomUUID() + "\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errors.code").value("NOT_FOUND"));
  }

  private MvcResult balanceApi() throws Exception {
    return mockMvc
        .perform(
            get("/api/inventory/stock-balances")
                .param("warehouseId", warehouseId)
                .param("itemTypeId", itemTypeId)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andReturn();
  }

  private String createCount(String marker) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/inventory/inventory-counts")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"company_id\":\""
                            + companyId
                            + "\",\"warehouse_id\":\""
                            + warehouseId
                            + "\",\"reference\":\""
                            + marker
                            + "\",\"notes\":\""
                            + marker
                            + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(notNullValue()))
            .andReturn();
    return JsonPath.read(body(result), "$.id");
  }

  private String insertItemType(Connection conn) throws Exception {
    try (PreparedStatement stmt =
        conn.prepareStatement(
            "INSERT INTO security.item_type (company_id, code, name) VALUES (?, ?, ?) RETURNING id")) {
      stmt.setObject(1, UUID.fromString(companyId));
      stmt.setString(2, TYPE_CODE);
      stmt.setString(3, TYPE_CODE);
      try (ResultSet rs = stmt.executeQuery()) {
        rs.next();
        return rs.getString(1);
      }
    }
  }

  private String insertItem(String qrCode, String warehouse) throws Exception {
    try (Connection conn = DatabaseConnection.getConnection()) {
      String id;
      try (PreparedStatement stmt =
          conn.prepareStatement(
              "INSERT INTO security.item "
                  + "(company_id, item_type_id, qr_code, status, current_warehouse_id) "
                  + "VALUES (?, ?, ?, 'AVAILABLE', ?) RETURNING id")) {
        stmt.setObject(1, UUID.fromString(companyId));
        stmt.setObject(2, UUID.fromString(itemTypeId));
        stmt.setString(3, qrCode);
        stmt.setObject(4, warehouse == null ? null : UUID.fromString(warehouse));
        try (ResultSet rs = stmt.executeQuery()) {
          rs.next();
          id = rs.getString(1);
        }
      }
      if (warehouse != null) {
        addBalance(conn, warehouse, BigDecimal.ONE);
      }
      return id;
    }
  }

  private void addBalance(Connection conn, String warehouse, BigDecimal quantity) throws Exception {
    try (PreparedStatement stmt =
        conn.prepareStatement(
            "INSERT INTO security.stock_balance "
                + "(company_id, warehouse_id, item_type_id, quantity) VALUES (?, ?, ?, ?) "
                + "ON CONFLICT (company_id, warehouse_id, item_type_id) DO UPDATE SET "
                + "quantity = security.stock_balance.quantity + EXCLUDED.quantity, "
                + "version = security.stock_balance.version + 1, updated_at = now()")) {
      stmt.setObject(1, UUID.fromString(companyId));
      stmt.setObject(2, UUID.fromString(warehouse));
      stmt.setObject(3, UUID.fromString(itemTypeId));
      stmt.setBigDecimal(4, quantity);
      stmt.executeUpdate();
    }
  }

  private BigDecimal balance(String warehouse) throws Exception {
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt =
            conn.prepareStatement(
                "SELECT quantity FROM security.stock_balance "
                    + "WHERE company_id = ? AND warehouse_id = ? AND item_type_id = ?")) {
      stmt.setObject(1, UUID.fromString(companyId));
      stmt.setObject(2, UUID.fromString(warehouse));
      stmt.setObject(3, UUID.fromString(itemTypeId));
      try (ResultSet rs = stmt.executeQuery()) {
        return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
      }
    }
  }

  private java.util.Map<String, Object> itemState(String id) throws Exception {
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt =
            conn.prepareStatement(
                "SELECT status, current_warehouse_id, last_inventory_at "
                    + "FROM security.item WHERE id = ?")) {
      stmt.setObject(1, UUID.fromString(id));
      try (ResultSet rs = stmt.executeQuery()) {
        assertTrue(rs.next(), "l'article doit exister");
        java.util.Map<String, Object> state = new java.util.HashMap<>();
        state.put("status", rs.getString("status"));
        state.put("current_warehouse_id", rs.getString("current_warehouse_id"));
        Timestamp lastInventoryAt = rs.getTimestamp("last_inventory_at");
        state.put("last_inventory_at", lastInventoryAt);
        return state;
      }
    }
  }

  private int count(String sql, Object... parameters) throws Exception {
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      for (int i = 0; i < parameters.length; i++) stmt.setObject(i + 1, parameters[i]);
      try (ResultSet rs = stmt.executeQuery()) {
        rs.next();
        return rs.getInt(1);
      }
    }
  }

  private static void assertDecimal(MvcResult result, String path, String expected)
      throws Exception {
    BigDecimal actual =
        new BigDecimal(String.valueOf((Object) JsonPath.read(body(result), path)));
    assertEquals(0, amount(expected).compareTo(actual), path + " doit valoir " + expected);
  }

  private static BigDecimal amount(String value) {
    return new BigDecimal(value);
  }

  private static String body(MvcResult result) throws Exception {
    return result.getResponse().getContentAsString();
  }

  private static String firstId(Connection conn, String sql) throws Exception {
    List<String> found = values(conn, sql);
    return found.isEmpty() ? null : found.get(0);
  }

  private static List<String> values(Connection conn, String sql) throws Exception {
    java.util.List<String> result = new java.util.ArrayList<>();
    try (PreparedStatement stmt = conn.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) result.add(rs.getString(1));
    }
    return result;
  }

  private static void purge() throws Exception {
    execute(
        "DELETE FROM security.inventory_count_scan WHERE inventory_count_id IN "
            + "(SELECT id FROM security.inventory_count WHERE notes LIKE 'phase3-lot3-%')");
    execute("DELETE FROM security.inventory_count WHERE notes LIKE 'phase3-lot3-%'");
    execute(
        "DELETE FROM security.inventory_operation_item WHERE operation_id IN "
            + "(SELECT id FROM security.inventory_operation "
            + "WHERE notes LIKE 'phase3-lot3-%' OR reference LIKE 'phase3-lot3-%')");
    execute(
        "DELETE FROM security.stock_movement WHERE notes LIKE 'phase3-lot3-%' "
            + "OR operation_id IN (SELECT id FROM security.inventory_operation "
            + "WHERE notes LIKE 'phase3-lot3-%' OR reference LIKE 'phase3-lot3-%')");
    execute(
        "DELETE FROM security.inventory_operation "
            + "WHERE notes LIKE 'phase3-lot3-%' OR reference LIKE 'phase3-lot3-%'");
    execute(
        "DELETE FROM security.item_assignment WHERE item_id IN "
            + "(SELECT id FROM security.item WHERE item_type_id IN "
            + "(SELECT id FROM security.item_type WHERE code = '" + TYPE_CODE + "'))");
    execute(
        "DELETE FROM security.stock_balance WHERE item_type_id IN "
            + "(SELECT id FROM security.item_type WHERE code = '" + TYPE_CODE + "')");
    execute(
        "DELETE FROM security.item WHERE item_type_id IN "
            + "(SELECT id FROM security.item_type WHERE code = '" + TYPE_CODE + "')");
    execute("DELETE FROM security.item_type WHERE code = '" + TYPE_CODE + "'");
  }

  private static void execute(String sql) throws Exception {
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.executeUpdate();
    }
  }
}
