package sssm.dev.sssm_backend.a11_inventory;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.Filter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
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

  @Autowired private WebApplicationContext context;
  @Autowired private JwtService jwtService;

  private MockMvc mockMvc;
  private String token;
  private String warehouseId;
  private String itemId;
  private String originalStatus;
  private String originalWarehouseId;

  @BeforeEach
  void setUp() throws Exception {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
            .build();
    token = jwtService.createToken("test-user", "timestamp-tester", List.of("ADMIN"));

    try (Connection conn = DatabaseConnection.getConnection()) {
      warehouseId = firstId(conn, "SELECT id FROM security.warehouse ORDER BY id LIMIT 1");
      // Le test OUT exige un article disponible ; l'ordre des UUID ne garantit pas ce statut.
      itemId =
          firstId(
              conn,
              "SELECT id FROM security.item WHERE status = 'AVAILABLE' ORDER BY id LIMIT 1");
      if (itemId != null) {
        try (PreparedStatement stmt =
            conn.prepareStatement(
                "SELECT status, current_warehouse_id FROM security.item WHERE id = ?")) {
          stmt.setObject(1, java.util.UUID.fromString(itemId));
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
        warehouseId != null && itemId != null,
        "base initialisée avec des données de démonstration requise");
  }

  @AfterEach
  void cleanUp() throws Exception {
    execute("DELETE FROM security.stock_movement WHERE notes LIKE 'phase2-ts-%'");
    // Les mouvements OUT modifient l'article partagé : restaurer l'état initial.
    if (itemId != null && originalStatus != null) {
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
                : java.util.UUID.fromString(originalWarehouseId));
        stmt.setObject(3, java.util.UUID.fromString(itemId));
        stmt.executeUpdate();
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
        .andExpect(jsonPath("$.movement_date").value(nullValue()));

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
            + "\",\"movement_type\":\"OUT\",\"movement_date\":\""
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
