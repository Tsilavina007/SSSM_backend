package sssm.dev.sssm_backend.a11_inventory;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.Filter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a03_authentication.auth.service.JwtService;

@SpringBootTest
class StockMovementTimestampTests {

  private static final String TYPE_CODE = "phase2-ts-type";
  private static final String ITEM_QR = "phase2-ts-item";

  @Autowired private WebApplicationContext context;
  @Autowired private JwtService jwtService;

  private MockMvc mockMvc;
  private String token;
  private String companyId;
  private String itemTypeId;
  private String warehouseId;
  private String itemId;
  private String employeeId;
  private String postId;

  @BeforeEach
  void setUp() throws Exception {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
            .build();
    token = jwtService.createToken("test-user", "timestamp-tester", List.of("ADMIN"));

    try (Connection conn = DatabaseConnection.getConnection()) {
      companyId = firstId(conn, "SELECT id FROM security.company ORDER BY id LIMIT 1");
      warehouseId = firstId(conn, "SELECT id FROM security.warehouse ORDER BY id LIMIT 1");
      employeeId = firstId(conn, "SELECT id FROM security.employee ORDER BY id LIMIT 1");
      postId = firstId(conn, "SELECT id FROM security.security_post ORDER BY id LIMIT 1");
    }
    Assumptions.assumeTrue(
        companyId != null && warehouseId != null && employeeId != null && postId != null,
        "base initialisée avec des données de démonstration requise");

    purge();
    try (Connection conn = DatabaseConnection.getConnection()) {
      itemTypeId = insertItemType(conn, companyId);
      itemId = insertItem(conn, companyId, itemTypeId, warehouseId);
      insertBalance(conn, companyId, warehouseId, itemTypeId);
    }
  }

  @AfterEach
  void cleanUp() throws Exception {
    purge();
  }

  @Test
  void createWithoutMovementTypeAppliesInDefaultInResponse() throws Exception {
    String marker = "phase2-ts-type-" + System.nanoTime();
    String body =
        "{\"warehouse_id\":\""
            + warehouseId
            + "\",\"item_id\":\""
            + itemId
            + "\",\"notes\":\""
            + marker
            + "\"}";

    mockMvc
        .perform(
            post("/api/inventory/movements")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.movement_type").value("IN"))
        .andExpect(jsonPath("$.movement_date").value(notNullValue()));

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt =
            conn.prepareStatement(
                "SELECT movement_type FROM security.stock_movement WHERE notes = ?")) {
      stmt.setString(1, marker);
      try (ResultSet rs = stmt.executeQuery()) {
        assertTrue(rs.next(), "le mouvement doit être créé");
        assertEquals("IN", rs.getString("movement_type"));
      }
    }
  }

  @Test
  void createWithoutMovementDateAppliesDatabaseDefault() throws Exception {
    String marker = "phase2-ts-absent-" + System.nanoTime();
    String body =
        "{\"warehouse_id\":\""
            + warehouseId
            + "\",\"item_id\":\""
            + itemId
            + "\",\"movement_type\":\"IN\",\"notes\":\""
            + marker
            + "\"}";

    mockMvc
        .perform(
            post("/api/inventory/movements")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(notNullValue()))
        .andExpect(jsonPath("$.movement_date").value(notNullValue()));

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt =
            conn.prepareStatement(
                "SELECT movement_date FROM security.stock_movement WHERE notes = ?")) {
      stmt.setString(1, marker);
      try (ResultSet rs = stmt.executeQuery()) {
        assertTrue(rs.next(), "la ligne doit être créée");
        assertNotNull(
            rs.getTimestamp("movement_date"),
            "movement_date doit être renseignée par le défaut PostgreSQL");
      }
    }
  }

  @Test
  void createWithExplicitMovementDateStoresProvidedValue() throws Exception {
    String marker = "phase2-ts-valid-" + System.nanoTime();
    String expected = "2030-01-15T10:30:00.000Z";
    String body =
        "{\"warehouse_id\":\""
            + warehouseId
            + "\",\"item_id\":\""
            + itemId
            + "\",\"movement_type\":\"OUT\",\"employee_id\":\""
            + employeeId
            + "\",\"post_id\":\""
            + postId
            + "\",\"movement_date\":\""
            + expected
            + "\",\"notes\":\""
            + marker
            + "\"}";

    mockMvc
        .perform(
            post("/api/inventory/movements")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(notNullValue()))
        .andExpect(jsonPath("$.movement_date").value(notNullValue()));

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt =
            conn.prepareStatement(
                "SELECT movement_date FROM security.stock_movement WHERE notes = ?")) {
      stmt.setString(1, marker);
      try (ResultSet rs = stmt.executeQuery()) {
        assertTrue(rs.next(), "la ligne doit être créée");
        Timestamp stored = rs.getTimestamp("movement_date");
        assertNotNull(stored, "movement_date doit être renseignée");
        assertEquals(
            Instant.parse(expected).toEpochMilli(),
            stored.getTime(),
            "movement_date doit correspondre à la valeur fournie");
      }
    }
  }

  @Test
  void createWithInvalidMovementDateReturnsBadRequest() throws Exception {
    String body =
        "{\"warehouse_id\":\""
            + warehouseId
            + "\",\"item_id\":\""
            + itemId
            + "\",\"movement_date\":\"not-a-timestamp\"}";

    mockMvc
        .perform(
            post("/api/inventory/movements")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());
  }

  private static String insertItemType(Connection conn, String companyId) throws Exception {
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

  private static String insertItem(
      Connection conn, String companyId, String itemTypeId, String warehouseId) throws Exception {
    try (PreparedStatement stmt =
        conn.prepareStatement(
            "INSERT INTO security.item "
                + "(company_id, item_type_id, qr_code, status, current_warehouse_id) "
                + "VALUES (?, ?, ?, 'AVAILABLE', ?) RETURNING id")) {
      stmt.setObject(1, UUID.fromString(companyId));
      stmt.setObject(2, UUID.fromString(itemTypeId));
      stmt.setString(3, ITEM_QR);
      stmt.setObject(4, UUID.fromString(warehouseId));
      try (ResultSet rs = stmt.executeQuery()) {
        rs.next();
        return rs.getString(1);
      }
    }
  }

  private static void insertBalance(
      Connection conn, String companyId, String warehouseId, String itemTypeId) throws Exception {
    try (PreparedStatement stmt =
        conn.prepareStatement(
            "INSERT INTO security.stock_balance "
                + "(company_id, warehouse_id, item_type_id, quantity) VALUES (?, ?, ?, ?)")) {
      stmt.setObject(1, UUID.fromString(companyId));
      stmt.setObject(2, UUID.fromString(warehouseId));
      stmt.setObject(3, UUID.fromString(itemTypeId));
      stmt.setBigDecimal(4, BigDecimal.ONE);
      stmt.executeUpdate();
    }
  }

  private static String firstId(Connection conn, String sql) throws Exception {
    try (PreparedStatement stmt = conn.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()) {
      return rs.next() ? rs.getString(1) : null;
    }
  }

  private static void purge() throws Exception {
    execute("DELETE FROM security.stock_movement WHERE notes LIKE 'phase2-ts-%'");
    execute(
        "DELETE FROM security.item_assignment WHERE item_id IN "
            + "(SELECT id FROM security.item WHERE qr_code = '" + ITEM_QR + "')");
    execute(
        "DELETE FROM security.stock_balance WHERE item_type_id IN "
            + "(SELECT id FROM security.item_type WHERE code = '" + TYPE_CODE + "')");
    execute("DELETE FROM security.item WHERE qr_code = '" + ITEM_QR + "'");
    execute("DELETE FROM security.item_type WHERE code = '" + TYPE_CODE + "'");
  }

  private static void execute(String sql) throws Exception {
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.executeUpdate();
    }
  }
}
