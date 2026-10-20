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

/** Routes du module warehouse : lecture, création, mise à jour, suppression. */
@SpringBootTest
class WarehouseApiTests {

  @Autowired private WebApplicationContext context;
  @Autowired private JwtService jwtService;

  private MockMvc mockMvc;
  private String token;
  private String companyId;
  private String branchId;
  private String warehouseId;

  @BeforeEach
  void setUp() throws Exception {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
            .build();
    token = jwtService.createToken("test-user", "phase5-tester", List.of("ADMIN"));

    try (Connection conn = DatabaseConnection.getConnection()) {
      companyId = firstValue(conn, "SELECT id FROM security.company ORDER BY id LIMIT 1");
      branchId = firstValue(conn, "SELECT id FROM security.branch ORDER BY id LIMIT 1");
      warehouseId = firstValue(conn, "SELECT id FROM security.warehouse ORDER BY id LIMIT 1");
    }
    Assumptions.assumeTrue(
        companyId != null && warehouseId != null,
        "base initialisée avec des données de démonstration requise");
    purge();
  }

  @AfterEach
  void cleanUp() throws Exception {
    purge();
  }

  @Test
  void listReturnsWarehouses() throws Exception {
    mockMvc
        .perform(get("/api/inventory/warehouses").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(notNullValue()));
  }

  @Test
  void getByIdReturnsWarehouse() throws Exception {
    mockMvc
        .perform(
            get("/api/inventory/warehouses/" + warehouseId)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(warehouseId));
  }

  @Test
  void createUpdateDeleteLifecycle() throws Exception {
    Assumptions.assumeTrue(branchId != null, "branche de démonstration requise");
    String marker = "phase5-wh-" + System.nanoTime();
    String body =
        "{\"company_id\":\""
            + companyId
            + "\",\"branch_id\":\""
            + branchId
            + "\",\"name\":\""
            + marker
            + "\",\"address\":\"rue phase5\",\"active\":true}";

    MvcResult created =
        mockMvc
            .perform(
                post("/api/inventory/warehouses")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(notNullValue()))
            .andExpect(jsonPath("$.name").value(marker))
            .andExpect(jsonPath("$.active").value(true))
            .andReturn();
    String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
    assertNotNull(id);

    // preuve SQL
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt =
            conn.prepareStatement("SELECT address FROM security.warehouse WHERE id = ?")) {
      stmt.setObject(1, UUID.fromString(id));
      try (ResultSet rs = stmt.executeQuery()) {
        assertTrue(rs.next(), "l'entrepôt créé doit exister en base");
        assertEquals("rue phase5", rs.getString("address"));
      }
    }

    mockMvc
        .perform(
            patch("/api/inventory/warehouses/" + id)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"address\":\"avenue phase5\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.address").value("avenue phase5"));

    mockMvc
        .perform(
            delete("/api/inventory/warehouses/" + id).header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            get("/api/inventory/warehouses/" + id).header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound());
  }

  @Test
  void unknownIdReturnsNotFoundEnvelope() throws Exception {
    mockMvc
        .perform(
            get("/api/inventory/warehouses/" + UUID.randomUUID())
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errors.message").value(notNullValue()));
  }

  @Test
  void deleteUnknownIsIdempotent() throws Exception {
    mockMvc
        .perform(
            delete("/api/inventory/warehouses/" + UUID.randomUUID())
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent());
  }

  private static void purge() throws Exception {
    execute("DELETE FROM security.warehouse WHERE name LIKE 'phase5-wh-%'");
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
