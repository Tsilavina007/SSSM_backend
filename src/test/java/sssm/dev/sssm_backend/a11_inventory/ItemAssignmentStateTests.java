package sssm.dev.sssm_backend.a11_inventory;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import java.util.HashMap;
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

/** Synchronisation atomique de l'état de l'article lors des affectations et restitutions. */
@SpringBootTest
class ItemAssignmentStateTests {

  private static final String TYPE_CODE = "phase4-asgstate-type";

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
    token = jwtService.createToken(actorId, "phase4-asgstate-tester", List.of("ADMIN"));

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
  void assignmentMarksItemAssignedAndClearsWarehouse() throws Exception {
    String itemId = createItem();
    createTypedAssignment(itemId, "phase4-asgstate-typed-" + System.nanoTime());

    Map<String, String> state = itemState(itemId);
    assertEquals("ASSIGNED", state.get("status"));
    assertNull(state.get("current_warehouse_id"));
  }

  @Test
  void secondActiveAssignmentIsRejectedWithoutSideEffect() throws Exception {
    String itemId = createItem();
    String marker = "phase4-asgstate-dup-" + System.nanoTime();
    createTypedAssignment(itemId, marker);

    mockMvc
        .perform(
            post("/api/item-assignments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(assignmentBody(itemId, marker + "-bis")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errors.code").value("ITEM_NOT_AVAILABLE"));

    assertEquals(1, countAssignments(itemId), "une seule affectation active doit exister");
    assertEquals("ASSIGNED", itemState(itemId).get("status"));
  }

  @Test
  void returnRestoresItemAvailabilityAndWarehouse() throws Exception {
    String itemId = createItem();
    String assignmentId =
        createTypedAssignment(itemId, "phase4-asgstate-return-" + System.nanoTime());

    mockMvc
        .perform(
            post("/api/item-assignments/" + assignmentId + "/return")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.returned_at").value(notNullValue()));

    Map<String, String> state = itemState(itemId);
    assertEquals("AVAILABLE", state.get("status"));
    assertEquals(warehouseId, state.get("current_warehouse_id"));
    assertEquals(0, amount("1").compareTo(balance()));
  }

  @Test
  void notesOnlyPatchDoesNotChangeItemState() throws Exception {
    String itemId = createItem();
    String assignmentId =
        createTypedAssignment(itemId, "phase4-asgstate-patch-" + System.nanoTime());

    mockMvc
        .perform(
            patch("/api/item-assignments/" + assignmentId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"notes\":\"mise a jour sans retour\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.returned_at").doesNotExist());

    assertEquals("ASSIGNED", itemState(itemId).get("status"));
    assertEquals(1, countAssignments(itemId));
  }

  @Test
  void genericAssignmentEndpointAlsoSynchronizesItemState() throws Exception {
    String itemId = createItem();
    String marker = "phase4-asgstate-generic-" + System.nanoTime();

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

    assertEquals("ASSIGNED", itemState(itemId).get("status"));

    String assignmentId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
    mockMvc
        .perform(
            post("/api/inventory/assignments/" + assignmentId + "/return")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());

    assertEquals("AVAILABLE", itemState(itemId).get("status"));
  }

  private String createItem() throws Exception {
    String marker = "phase4-asgstate-" + System.nanoTime();
    String body =
        "{\"company_id\":\""
            + companyId
            + "\",\"item_type_id\":\""
            + itemTypeId
            + "\",\"current_warehouse_id\":\""
            + warehouseId
            + "\",\"qr_code\":\""
            + marker
            + "\",\"description\":\""
            + marker
            + "\",\"status\":\"AVAILABLE\"}";

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

  private String createTypedAssignment(String itemId, String marker) throws Exception {
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
                "SELECT status, current_warehouse_id FROM security.item WHERE id = ?")) {
      stmt.setObject(1, UUID.fromString(id));
      try (ResultSet rs = stmt.executeQuery()) {
        assertTrue(rs.next(), "l'article doit exister");
        Map<String, String> state = new HashMap<>();
        state.put("status", rs.getString("status"));
        state.put("current_warehouse_id", rs.getString("current_warehouse_id"));
        return state;
      }
    }
  }

  private static int countAssignments(String itemId) throws Exception {
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
        "DELETE FROM security.item_assignment WHERE item_id IN "
            + "(SELECT id FROM security.item WHERE item_type_id IN "
            + "(SELECT id FROM security.item_type WHERE code = '" + TYPE_CODE + "'))");
    execute(
        "DELETE FROM security.stock_movement WHERE item_id IN "
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
