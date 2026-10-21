package sssm.dev.sssm_backend.a11_inventory;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

/**
 * Routes du module assignment (famille générique à corps Map, sous /api/inventory/assignments).
 * Les erreurs douces de cette famille répondent en 200 avec {"errors": ItemError} et jamais 404 sur
 * les écritures — comportements vérifiés tels quels ici.
 */
@SpringBootTest
class AssignmentApiTests {

  @Autowired private WebApplicationContext context;
  @Autowired private JwtService jwtService;

  private MockMvc mockMvc;
  private String token;
  private String employeeId;
  private String postId;
  private String unassignedItemId;
  private String originalStatus;
  private String originalWarehouseId;

  @BeforeEach
  void setUp() throws Exception {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
            .build();
    token = jwtService.createToken("test-user", "phase5-tester", List.of("ADMIN"));

    purge();
    try (Connection conn = DatabaseConnection.getConnection()) {
      employeeId = firstValue(conn, "SELECT id FROM security.employee ORDER BY id LIMIT 1");
      postId = firstValue(conn, "SELECT id FROM security.security_post ORDER BY id LIMIT 1");
      // article sans affectation active (contrainte uq_item_assignment_active)
      unassignedItemId =
          firstValue(
              conn,
              "SELECT i.id FROM security.item i WHERE NOT EXISTS ("
                  + "SELECT 1 FROM security.item_assignment a"
                  + " WHERE a.item_id = i.id AND a.returned_at IS NULL)"
                  + " ORDER BY i.id LIMIT 1");
      if (unassignedItemId != null) {
        try (PreparedStatement stmt =
            conn.prepareStatement(
                "SELECT status, current_warehouse_id FROM security.item WHERE id = ?")) {
          stmt.setObject(1, UUID.fromString(unassignedItemId));
          try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
              originalStatus = rs.getString("status");
              originalWarehouseId = rs.getString("current_warehouse_id");
            }
          }
        }
      }
    }
    Assumptions.assumeTrue(
        employeeId != null && postId != null,
        "base initialisée avec des données de démonstration requise");
  }

  @AfterEach
  void cleanUp() throws Exception {
    purge();
    if (unassignedItemId != null && originalStatus != null) {
      try (Connection conn = DatabaseConnection.getConnection();
          PreparedStatement stmt =
              conn.prepareStatement(
                  "UPDATE security.item SET status = CAST(? AS security.item_status),"
                      + " current_warehouse_id = ? WHERE id = ?")) {
        stmt.setString(1, originalStatus);
        stmt.setObject(
            2,
            originalWarehouseId == null
                ? null
                : UUID.fromString(originalWarehouseId));
        stmt.setObject(3, UUID.fromString(unassignedItemId));
        stmt.executeUpdate();
      }
    }
  }

  @Test
  void listReturnsActiveAssignments() throws Exception {
    MvcResult result =
        mockMvc
            .perform(get("/api/inventory/assignments").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();
    List<?> rows = JsonPath.read(result.getResponse().getContentAsString(), "$");
    if (!rows.isEmpty()) {
      assertNotNull(((Map<?, ?>) rows.get(0)).get("id"), "chaque ligne porte un id");
    }
  }

  @Test
  void createPersistsRow() throws Exception {
    String id = createAssignment("phase5-asg-ok-" + System.nanoTime());
    assertNotNull(id);

    // preuve SQL : ligne active dans security.item_assignment
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt =
            conn.prepareStatement(
                "SELECT item_id, assigned_at FROM security.item_assignment WHERE id = ?")) {
      stmt.setObject(1, UUID.fromString(id));
      try (ResultSet rs = stmt.executeQuery()) {
        assertTrue(rs.next(), "l'affectation créée doit exister en base");
        assertEquals(unassignedItemId, rs.getString("item_id"));
        assertNotNull(rs.getTimestamp("assigned_at"), "assigned_at : défaut base now()");
      }
    }

    mockMvc
        .perform(
            get("/api/inventory/assignments/" + id).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id));
  }

  @Test
  void patchUpdatesNotes() throws Exception {
    String id = createAssignment("phase5-asg-patch");
    String updated = "phase5-asg-patch-upd-" + System.nanoTime();

    mockMvc
        .perform(
            patch("/api/inventory/assignments/" + id)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"notes\":\"" + updated + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.notes").value(updated));
  }

  @Test
  void returnRecordsCondition() throws Exception {
    String id = createAssignment("phase5-asg-return");

    mockMvc
        .perform(
            post("/api/inventory/assignments/" + id + "/return")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"condition_on_return\":\"NEUF\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.condition_on_return").value("NEUF"));
  }

  @Test
  void getUnknownReturnsSoftErrorIn200() throws Exception {
    mockMvc
        .perform(
            get("/api/inventory/assignments/" + UUID.randomUUID())
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.errors.message").value("Affectation d'article introuvable"))
        .andExpect(jsonPath("$.errors.detail").value(notNullValue()))
        .andExpect(jsonPath("$.errors.code").value("NOT_FOUND"));
  }

  @Test
  void emptyBodyReturnsSoftErrorIn200() throws Exception {
    mockMvc
        .perform(
            post("/api/inventory/assignments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.errors.message").value("Corps de requête obligatoire"))
        .andExpect(jsonPath("$.errors.detail").value(notNullValue()))
        .andExpect(jsonPath("$.errors.code").value("BAD_REQUEST"));
  }

  @Test
  void missingRequiredColumnReturns500Envelope() throws Exception {
    // dette connue : colonnes NOT NULL omises → 500 (et non 400) en famille C
    mockMvc
        .perform(
            post("/api/inventory/assignments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"notes\":\"sans ids\"}"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.errors.message").value("Champ obligatoire manquant"))
        .andExpect(jsonPath("$.errors.detail").value(containsString("SQLState")))
        .andExpect(jsonPath("$.errors.code").value("23502"));
  }

  @Test
  void byEmployeeReturnsCreatedAssignment() throws Exception {
    String marker = "phase5-asg-emp-" + System.nanoTime();
    createAssignment(marker);

    mockMvc
        .perform(
            get("/api/inventory/employees/" + employeeId + "/assignments")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(marker)));
  }

  @Test
  void byPostReturnsCreatedAssignment() throws Exception {
    String marker = "phase5-asg-post-" + System.nanoTime();
    createAssignment(marker);

    mockMvc
        .perform(
            get("/api/inventory/security-posts/" + postId + "/assignments")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(marker)));
  }

  private String createAssignment(String marker) throws Exception {
    Assumptions.assumeTrue(
        unassignedItemId != null,
        "aucun article non affecté disponible (pool épuisé ?)");
    String body =
        "{\"item_id\":\""
            + unassignedItemId
            + "\",\"employee_id\":\""
            + employeeId
            + "\",\"post_id\":\""
            + postId
            + "\",\"notes\":\""
            + marker
            + "\"}";

    MvcResult result =
        mockMvc
            .perform(
                post("/api/inventory/assignments")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(notNullValue()))
            .andExpect(jsonPath("$.notes").value(marker))
            .andExpect(jsonPath("$.assigned_at").value(notNullValue()))
            .andReturn();
    return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
  }

  private static void purge() throws Exception {
    execute("DELETE FROM security.item_assignment WHERE notes LIKE 'phase5-asg%'");
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
