package sssm.dev.sssm_backend.a11_inventory;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.Filter;
import java.math.BigDecimal;
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

/**
 * Lot 4 : contrat canonique des affectations, retour, void, archivage et façade dépréciée.
 *
 * <p>Chaque test dispose de son propre type d'article et de son article afin que le solde
 * disponible soit explicite et que les données de démonstration ne soient jamais réutilisées.
 */
@SpringBootTest
class AssignmentApiTests {

  private static final String TYPE_CODE = "phase4-asg-type";

  @Autowired private WebApplicationContext context;
  @Autowired private JwtService jwtService;

  private MockMvc mockMvc;
  private String token;
  private String actorId;
  private String companyId;
  private String warehouseId;
  private String itemTypeId;
  private String employeeId;
  private String postId;

  @BeforeEach
  void setUp() throws Exception {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
            .build();
    actorId = UUID.randomUUID().toString();
    token = jwtService.createToken(actorId, "lot4-assignment-tester", List.of("ADMIN"));

    purge();
    try (Connection conn = DatabaseConnection.getConnection()) {
      companyId = firstValue(conn, "SELECT id FROM security.company ORDER BY id LIMIT 1");
      warehouseId = firstValue(conn, "SELECT id FROM security.warehouse ORDER BY id LIMIT 1");
      employeeId = firstValue(conn, "SELECT id FROM security.employee ORDER BY id LIMIT 1");
      postId = firstValue(conn, "SELECT id FROM security.security_post ORDER BY id LIMIT 1");
      if (companyId != null) {
        itemTypeId = insertItemType(conn);
      }
    }
    Assumptions.assumeTrue(
        companyId != null
            && warehouseId != null
            && itemTypeId != null
            && employeeId != null
            && postId != null,
        "base initialisée avec des données de démonstration requise");
  }

  @AfterEach
  void cleanUp() throws Exception {
    purge();
  }

  @Test
  void createPersistsActiveAssignmentAndSynchronizesItemAndBalance() throws Exception {
    String itemId = createItem("AVAILABLE", warehouseId);
    String marker = "phase4-asg-create-" + System.nanoTime();

    MvcResult created =
        mockMvc
            .perform(
                post("/api/item-assignments")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(assignmentBody(itemId, marker)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(notNullValue()))
            .andExpect(jsonPath("$.item_id").value(itemId))
            .andExpect(jsonPath("$.notes").value(marker))
            .andExpect(jsonPath("$.assigned_at").value(notNullValue()))
            .andExpect(jsonPath("$.assigned_by").value(actorId))
            .andExpect(jsonPath("$.source_warehouse_id").value(warehouseId))
            .andReturn();
    String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

    assertEquals("ASSIGNED", itemState(itemId).get("status"));
    assertNull(itemState(itemId).get("current_warehouse_id"));
    assertEquals(1, countActive(itemId));
    assertEquals(0, amount("0").compareTo(balance()));
    assertEquals(warehouseId, assignmentRow(id).get("source_warehouse_id"));
    assertNull(assignmentRow(id).get("returned_at"));

    mockMvc
        .perform(
            get("/api/item-assignments/" + id).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id));
  }

  @Test
  void unavailableItemReturnsConflict() throws Exception {
    String itemId = createItem("ASSIGNED", null);

    mockMvc
        .perform(
            post("/api/item-assignments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(assignmentBody(itemId, "phase4-asg-unavailable-" + System.nanoTime())))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errors.message").value("Article non disponible"))
        .andExpect(jsonPath("$.errors.code").value("ITEM_NOT_AVAILABLE"));

    assertEquals(0, countActive(itemId));
  }

  @Test
  void unknownAssignmentReturnsNotFound() throws Exception {
    mockMvc
        .perform(
            get("/api/item-assignments/" + UUID.randomUUID())
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errors.code").value("NOT_FOUND"))
        .andExpect(jsonPath("$.errors.detail").value(notNullValue()));
  }

  @Test
  void invalidCreateReturnsBadRequestWithoutSqlDetails() throws Exception {
    mockMvc
        .perform(
            post("/api/item-assignments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"notes\":\"sans identifiants\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors.detail").value(not(containsString("SQLState"))));
  }

  @Test
  void notesOnlyPatchDoesNotChangeLifecycle() throws Exception {
    String itemId = createItem("AVAILABLE", warehouseId);
    String id = createAssignment(itemId, "phase4-asg-notes-" + System.nanoTime());
    String updated = "phase4-asg-notes-upd-" + System.nanoTime();

    mockMvc
        .perform(
            patch("/api/item-assignments/" + id)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"notes\":\"" + updated + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.notes").value(updated))
        .andExpect(jsonPath("$.returned_at").doesNotExist());

    assertEquals("ASSIGNED", itemState(itemId).get("status"));
    assertEquals(1, countActive(itemId));
    assertNull(assignmentRow(id).get("returned_at"));
  }

  @Test
  void conditionOnlyPatchIsRejected() throws Exception {
    String itemId = createItem("AVAILABLE", warehouseId);
    String id = createAssignment(itemId, "phase4-asg-condition-" + System.nanoTime());

    mockMvc
        .perform(
            patch("/api/item-assignments/" + id)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"condition_on_return\":\"NEUF\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.code").value("RETURN_ENDPOINT_REQUIRED"));

    assertEquals(1, countActive(itemId));
    assertNull(assignmentRow(id).get("returned_at"));
  }

  @Test
  void dedicatedReturnClosesAssignmentAndRestoresWarehouse() throws Exception {
    String itemId = createItem("AVAILABLE", warehouseId);
    String id = createAssignment(itemId, "phase4-asg-return-" + System.nanoTime());

    mockMvc
        .perform(
            post("/api/item-assignments/" + id + "/return")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"condition_on_return\":\"NEUF\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.returned_at").value(notNullValue()))
        .andExpect(jsonPath("$.returned_by").value(actorId))
        .andExpect(jsonPath("$.condition_on_return").value("NEUF"));

    assertEquals("AVAILABLE", itemState(itemId).get("status"));
    assertEquals(warehouseId, itemState(itemId).get("current_warehouse_id"));
    assertEquals(0, countActive(itemId));
    assertEquals(0, amount("1").compareTo(balance()));

    mockMvc
        .perform(
            post("/api/item-assignments/" + id + "/return")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errors.code").value("ASSIGNMENT_ALREADY_RETURNED"));
  }

  @Test
  void patchReturnedAtDelegatesToServerReturn() throws Exception {
    String itemId = createItem("AVAILABLE", warehouseId);
    String id = createAssignment(itemId, "phase4-asg-patch-return-" + System.nanoTime());

    mockMvc
        .perform(
            patch("/api/item-assignments/" + id)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"returned_at\":\"2030-01-15T10:30:00.000Z\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.returned_at").value(notNullValue()))
        .andExpect(jsonPath("$.returned_by").value(actorId));

    assertEquals("AVAILABLE", itemState(itemId).get("status"));
    assertEquals(0, countActive(itemId));
  }

  @Test
  void activeAssignmentCannotBeDeleted() throws Exception {
    String itemId = createItem("AVAILABLE", warehouseId);
    String id = createAssignment(itemId, "phase4-asg-delete-active-" + System.nanoTime());

    mockMvc
        .perform(
            delete("/api/item-assignments/" + id)
                .header("Authorization", "Bearer " + token)
                .param("reason", "tentative"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errors.code").value("ACTIVE_ASSIGNMENT"));

    assertEquals(1, countActive(itemId));
    assertNull(assignmentRow(id).get("voided_at"));
  }

  @Test
  void returnedAssignmentCanBeArchivedAndRestored() throws Exception {
    String itemId = createItem("AVAILABLE", warehouseId);
    String marker = "phase4-asg-archive-" + System.nanoTime();
    String id = createAssignment(itemId, marker);
    returnAssignment(id);

    mockMvc
        .perform(
            delete("/api/item-assignments/" + id)
                .header("Authorization", "Bearer " + token)
                .param("reason", "nettoyage historique"))
        .andExpect(status().isNoContent());

    Map<String, String> archived = assignmentRow(id);
    assertNotNull(archived.get("voided_at"));
    assertEquals("nettoyage historique", archived.get("void_reason"));

    mockMvc
        .perform(
            get("/api/item-assignments")
                .param("search", marker)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));

    mockMvc
        .perform(
            get("/api/item-assignments/" + id).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.voided_at").value(notNullValue()));

    mockMvc
        .perform(
            post("/api/item-assignments/" + id + "/restore")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.voided_at").doesNotExist());

    assertNull(assignmentRow(id).get("voided_at"));
  }

  @Test
  void voidActiveAssignmentAuditsAndRestoresItem() throws Exception {
    String itemId = createItem("AVAILABLE", warehouseId);
    String marker = "phase4-asg-void-" + System.nanoTime();
    String id = createAssignment(itemId, marker);

    mockMvc
        .perform(
            post("/api/item-assignments/" + id + "/void")
                .header("Authorization", "Bearer " + token)
                .param("reason", "erreur de saisie"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.voided_at").value(notNullValue()))
        .andExpect(jsonPath("$.voided_by").value(actorId))
        .andExpect(jsonPath("$.void_reason").value("erreur de saisie"));

    assertEquals("AVAILABLE", itemState(itemId).get("status"));
    assertEquals(warehouseId, itemState(itemId).get("current_warehouse_id"));
    assertEquals(0, countActive(itemId));

    mockMvc
        .perform(
            get("/api/item-assignments")
                .param("search", marker)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));

    mockMvc
        .perform(
            get("/api/item-assignments/item/" + itemId)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.id == '" + id + "')]").isEmpty());

    mockMvc
        .perform(
            post("/api/item-assignments/" + id + "/void")
                .header("Authorization", "Bearer " + token)
                .param("reason", "deuxieme fois"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errors.code").value("ASSIGNMENT_ALREADY_VOIDED"));
  }

  @Test
  void genericFacadeIsDeprecatedAndSharesContract() throws Exception {
    String itemId = createItem("AVAILABLE", warehouseId);
    String marker = "phase4-asg-facade-" + System.nanoTime();

    MvcResult created =
        mockMvc
            .perform(
                post("/api/inventory/assignments")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(assignmentBody(itemId, marker)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Deprecation", "true"))
            .andExpect(header().string("Link", containsString("/api/item-assignments")))
            .andExpect(jsonPath("$.id").value(notNullValue()))
            .andReturn();
    String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(
            get("/api/inventory/assignments/" + UUID.randomUUID())
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errors.code").value("NOT_FOUND"));

    mockMvc
        .perform(
            delete("/api/inventory/assignments/" + id)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errors.code").value("ACTIVE_ASSIGNMENT"));
  }

  @Test
  void employeePostAndActiveFiltersUseTheSameRows() throws Exception {
    String itemId = createItem("AVAILABLE", warehouseId);
    String marker = "phase4-asg-filter-" + System.nanoTime();
    String id = createAssignment(itemId, marker);

    mockMvc
        .perform(
            get("/api/inventory/employees/" + employeeId + "/assignments")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.id == '" + id + "')].notes").value(marker));

    mockMvc
        .perform(
            get("/api/inventory/security-posts/" + postId + "/assignments")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.id == '" + id + "')].notes").value(marker));

    mockMvc
        .perform(
            get("/api/item-assignments")
                .param("search", marker)
                .param("active", "true")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));

    returnAssignment(id);

    mockMvc
        .perform(
            get("/api/item-assignments")
                .param("search", marker)
                .param("active", "true")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));

    mockMvc
        .perform(
            get("/api/item-assignments")
                .param("search", marker)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void itemArchiveIsBlockedByActiveAssignmentThenCanBeRestored() throws Exception {
    String itemId = createItem("AVAILABLE", warehouseId);
    String marker = "phase4-asg-item-archive-" + System.nanoTime();
    String id = createAssignment(itemId, marker);

    mockMvc
        .perform(
            delete("/api/inventory/items/" + itemId)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errors.code").value("ITEM_ASSIGNED"));

    returnAssignment(id);

    mockMvc
        .perform(
            delete("/api/inventory/items/" + itemId)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent());

    assertNotNull(itemState(itemId).get("archived_at"));
    assertEquals(0, amount("0").compareTo(balance()));

    mockMvc
        .perform(
            get("/api/inventory/items")
                .param("search", marker)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));

    mockMvc
        .perform(
            post("/api/inventory/items/" + itemId + "/restore")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.archived_at").doesNotExist());

    assertNull(itemState(itemId).get("archived_at"));
    assertEquals(0, amount("1").compareTo(balance()));
  }

  private String createItem(String status, String warehouse) throws Exception {
    String marker = "phase4-asg-item-" + System.nanoTime();
    String body =
        "{\"company_id\":\""
            + companyId
            + "\",\"item_type_id\":\""
            + itemTypeId
            + "\",\"qr_code\":\""
            + marker
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
    return JsonPath.read(created.getResponse().getContentAsString(), "$.id");
  }

  private String createAssignment(String itemId, String marker) throws Exception {
    MvcResult created =
        mockMvc
            .perform(
                post("/api/item-assignments")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(assignmentBody(itemId, marker)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(notNullValue()))
            .andReturn();
    return JsonPath.read(created.getResponse().getContentAsString(), "$.id");
  }

  private void returnAssignment(String id) throws Exception {
    mockMvc
        .perform(
            post("/api/item-assignments/" + id + "/return")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.returned_at").value(notNullValue()));
  }

  private String assignmentBody(String itemId, String marker) {
    return "{\"item_id\":\""
        + itemId
        + "\",\"employee_id\":\""
        + employeeId
        + "\",\"post_id\":\""
        + postId
        + "\",\"notes\":\""
        + marker
        + "\"}";
  }

  private static Map<String, String> itemState(String id) throws Exception {
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt =
            conn.prepareStatement(
                "SELECT status, current_warehouse_id, archived_at FROM security.item WHERE id = ?")) {
      stmt.setObject(1, UUID.fromString(id));
      try (ResultSet rs = stmt.executeQuery()) {
        assertTrue(rs.next(), "l'article doit exister");
        Map<String, String> state = new java.util.HashMap<>();
        state.put("status", rs.getString("status"));
        state.put("current_warehouse_id", rs.getString("current_warehouse_id"));
        state.put("archived_at", rs.getString("archived_at"));
        return state;
      }
    }
  }

  private static Map<String, String> assignmentRow(String id) throws Exception {
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt =
            conn.prepareStatement(
                "SELECT source_warehouse_id, returned_at, voided_at, void_reason "
                    + "FROM security.item_assignment WHERE id = ?")) {
      stmt.setObject(1, UUID.fromString(id));
      try (ResultSet rs = stmt.executeQuery()) {
        assertTrue(rs.next(), "l'affectation doit exister");
        Map<String, String> row = new java.util.HashMap<>();
        row.put("source_warehouse_id", rs.getString("source_warehouse_id"));
        row.put("returned_at", rs.getString("returned_at"));
        row.put("voided_at", rs.getString("voided_at"));
        row.put("void_reason", rs.getString("void_reason"));
        return row;
      }
    }
  }

  private static int countActive(String itemId) throws Exception {
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt =
            conn.prepareStatement(
                "SELECT count(*) FROM security.item_assignment "
                    + "WHERE item_id = ? AND returned_at IS NULL AND voided_at IS NULL")) {
      stmt.setObject(1, UUID.fromString(itemId));
      try (ResultSet rs = stmt.executeQuery()) {
        rs.next();
        return rs.getInt(1);
      }
    }
  }

  private BigDecimal balance() throws Exception {
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt =
            conn.prepareStatement(
                "SELECT quantity FROM security.stock_balance "
                    + "WHERE item_type_id = ? AND warehouse_id = ?")) {
      stmt.setObject(1, UUID.fromString(itemTypeId));
      stmt.setObject(2, UUID.fromString(warehouseId));
      try (ResultSet rs = stmt.executeQuery()) {
        return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
      }
    }
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

  private static void purge() throws Exception {
    execute(
        "DELETE FROM security.stock_movement WHERE item_id IN "
            + "(SELECT id FROM security.item WHERE item_type_id IN "
            + "(SELECT id FROM security.item_type WHERE code = '" + TYPE_CODE + "'))");
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

  private static BigDecimal amount(String value) {
    return new BigDecimal(value);
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
