package sssm.dev.sssm_backend.a11_inventory;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

/** Synchronisation de l'état de l'article lors des affectations et restitutions. */
@SpringBootTest
class ItemAssignmentStateTests {

  @Autowired private WebApplicationContext context;
  @Autowired private JwtService jwtService;

  private MockMvc mockMvc;
  private String token;
  private String companyId;
  private String itemTypeId;
  private String employeeId;
  private String postId;

  @BeforeEach
  void setUp() throws Exception {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
            .build();
    token = jwtService.createToken("test-user", "phase5-asgstate-tester", List.of("ADMIN"));

    try (Connection conn = DatabaseConnection.getConnection()) {
      companyId = firstValue(conn, "SELECT id FROM security.company ORDER BY id LIMIT 1");
      itemTypeId = firstValue(conn, "SELECT id FROM security.item_type ORDER BY id LIMIT 1");
      employeeId = firstValue(conn, "SELECT id FROM security.employee ORDER BY id LIMIT 1");
      postId = firstValue(conn, "SELECT id FROM security.security_post ORDER BY id LIMIT 1");
    }
    Assumptions.assumeTrue(
        companyId != null
            && itemTypeId != null
            && employeeId != null
            && postId != null,
        "base initialisée avec des données de démonstration requise");
    purge();
  }

  @AfterEach
  void cleanUp() throws Exception {
    purge();
  }

  @Test
  void assignmentMarksItemAssignedAndClearsWarehouse() throws Exception {
    String itemId = createItem();

    createTypedAssignment(itemId, "phase5-asgstate-typed-" + System.nanoTime());

    Map<String, String> state = itemState(itemId);
    assertEquals("ASSIGNED", state.get("status"));
    assertNull(state.get("current_warehouse_id"));
  }

  @Test
  void secondActiveAssignmentIsRejected() throws Exception {
    String itemId = createItem();
    String marker = "phase5-asgstate-dup-" + System.nanoTime();
    createTypedAssignment(itemId, marker);

    mockMvc
        .perform(
            post("/api/item-assignments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(assignmentBody(itemId, marker + "-bis")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.message").value("Article non disponible"))
        .andExpect(jsonPath("$.errors.code").value("CLIENT_ERROR"));

    assertEquals(1, countAssignments(itemId), "une seule affectation active doit exister");
    assertEquals("ASSIGNED", itemState(itemId).get("status"));
  }

  @Test
  void returnRestoresItemAvailability() throws Exception {
    String itemId = createItem();
    String assignmentId =
        createTypedAssignment(itemId, "phase5-asgstate-return-" + System.nanoTime());

    mockMvc
        .perform(
            patch("/api/item-assignments/" + assignmentId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"returned_at\":\"2030-01-15T10:30:00.000Z\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.returned_at").value(notNullValue()));

    assertEquals("AVAILABLE", itemState(itemId).get("status"));
  }

  @Test
  void genericAssignmentEndpointAlsoSynchronizesItemState() throws Exception {
    String itemId = createItem();
    String marker = "phase5-asgstate-generic-" + System.nanoTime();

    MvcResult created =
        mockMvc
            .perform(
                post("/api/inventory/assignments")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(assignmentBody(itemId, marker)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(notNullValue()))
            .andReturn();

    assertEquals("ASSIGNED", itemState(itemId).get("status"));

    String assignmentId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
    mockMvc
        .perform(
            post("/api/inventory/assignments/" + assignmentId + "/return")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"returned_at\":\"2030-01-15T10:30:00.000Z\"}"))
        .andExpect(status().isOk());

    assertEquals("AVAILABLE", itemState(itemId).get("status"));
  }

  private String createItem() throws Exception {
    String marker = "phase5-asgstate-" + System.nanoTime();
    String body =
        "{\"company_id\":\""
            + companyId
            + "\",\"item_type_id\":\""
            + itemTypeId
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
                    + "WHERE item_id = ? AND returned_at IS NULL")) {
      stmt.setObject(1, UUID.fromString(itemId));
      try (ResultSet rs = stmt.executeQuery()) {
        rs.next();
        return rs.getInt(1);
      }
    }
  }

  private static void purge() throws Exception {
    execute(
        "DELETE FROM security.item_assignment WHERE item_id IN "
            + "(SELECT id FROM security.item WHERE description LIKE 'phase5-asgstate-%')");
    execute("DELETE FROM security.item WHERE description LIKE 'phase5-asgstate-%'");
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
