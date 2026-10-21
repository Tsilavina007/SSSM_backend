package sssm.dev.sssm_backend.a11_inventory;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

/** Route du module report : GET /api/inventory/reports/movements (lecture seule). */
@SpringBootTest
class ReportApiTests {

  @Autowired private WebApplicationContext context;
  @Autowired private JwtService jwtService;

  private MockMvc mockMvc;
  private String token;
  private String companyId;
  private String itemTypeId;
  private String warehouseId;
  private String itemId;

  @BeforeEach
  void setUp() throws Exception {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
            .build();
    token = jwtService.createToken("test-user", "phase5-tester", List.of("ADMIN"));

    try (Connection conn = DatabaseConnection.getConnection()) {
      companyId = firstValue(conn, "SELECT id FROM security.company ORDER BY id LIMIT 1");
      itemTypeId = firstValue(conn, "SELECT id FROM security.item_type ORDER BY id LIMIT 1");
      warehouseId = firstValue(conn, "SELECT id FROM security.warehouse ORDER BY id LIMIT 1");
      itemId = firstValue(conn, "SELECT id FROM security.item ORDER BY id LIMIT 1");
    }
    Assumptions.assumeTrue(
        warehouseId != null && itemId != null && companyId != null && itemTypeId != null,
        "base initialisée avec des données de démonstration requise");
    purge();
  }

  @AfterEach
  void cleanUp() throws Exception {
    purge();
  }

  @Test
  void defaultPaginationReturnsExistingMovements() throws Exception {
    mockMvc
        .perform(get("/api/inventory/reports/movements").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(notNullValue()));
  }

  @Test
  void reportContainsMovementCreatedByApi() throws Exception {
    String marker = "phase5-mv-" + System.nanoTime();

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
        .andExpect(jsonPath("$.id").value(notNullValue()));

    MvcResult result =
        mockMvc
            .perform(
                get("/api/inventory/reports/movements?pageSize=500")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(marker)))
            .andExpect(jsonPath("$[0].id").value(notNullValue()))
            .andReturn();

    List<?> rows = JsonPath.read(result.getResponse().getContentAsString(), "$");
    assertNotNull(((Map<?, ?>) rows.get(0)).get("movement_type"), "colonnes brutes exposées");
  }

  @Test
  void movementReportFiltersByItem() throws Exception {
    String marker = "phase5-mv-filter-item-" + System.nanoTime();
    String id = createItemWithMovement(marker);

    MvcResult result =
        mockMvc
            .perform(
                get("/api/inventory/reports/movements")
                    .param("itemId", id)
                    .param("pageSize", "500")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(marker)))
            .andReturn();

    List<String> itemIds = JsonPath.read(result.getResponse().getContentAsString(), "$[*].item_id");
    assertTrue(itemIds.stream().allMatch(id::equals), "seuls les mouvements de l'article");
  }

  @Test
  void movementReportFiltersByWarehouse() throws Exception {
    String marker = "phase5-mv-filter-wh-" + System.nanoTime();
    String id = createItemWithMovement(marker);

    MvcResult result =
        mockMvc
            .perform(
                get("/api/inventory/reports/movements")
                    .param("warehouseId", warehouseId)
                    .param("pageSize", "500")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(marker)))
            .andReturn();

    List<String> warehouseIds =
        JsonPath.read(result.getResponse().getContentAsString(), "$[*].warehouse_id");
    assertTrue(
        warehouseIds.stream().allMatch(warehouseId::equals), "seuls les mouvements de l'entrepôt");

    // le mouvement créé reste rattaché à l'article dédié
    List<String> itemIds = JsonPath.read(result.getResponse().getContentAsString(), "$[*].item_id");
    assertTrue(itemIds.contains(id));
  }

  @Test
  void movementReportCombinesItemAndWarehouseFilters() throws Exception {
    String marker = "phase5-mv-filter-both-" + System.nanoTime();
    String id = createItemWithMovement(marker);

    MvcResult result =
        mockMvc
            .perform(
                get("/api/inventory/reports/movements")
                    .param("itemId", id)
                    .param("warehouseId", warehouseId)
                    .param("pageSize", "500")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(marker)))
            .andReturn();

    List<String> itemIds = JsonPath.read(result.getResponse().getContentAsString(), "$[*].item_id");
    List<String> warehouseIds =
        JsonPath.read(result.getResponse().getContentAsString(), "$[*].warehouse_id");
    assertTrue(itemIds.stream().allMatch(id::equals));
    assertTrue(warehouseIds.stream().allMatch(warehouseId::equals));
  }

  private String createItemWithMovement(String marker) throws Exception {
    String body =
        "{\"company_id\":\""
            + companyId
            + "\",\"item_type_id\":\""
            + itemTypeId
            + "\",\"current_warehouse_id\":\""
            + warehouseId
            + "\",\"qr_code\":\""
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
    String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(
            post("/api/inventory/movements")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"warehouse_id\":\""
                        + warehouseId
                        + "\",\"item_id\":\""
                        + id
                        + "\",\"movement_type\":\"IN\",\"notes\":\""
                        + marker
                        + "\"}"))
        .andExpect(status().isCreated());
    return id;
  }

  private static void purge() throws Exception {
    execute(
        "DELETE FROM security.stock_movement WHERE notes LIKE 'phase5-mv-%'"
            + " OR item_id IN (SELECT id FROM security.item WHERE qr_code LIKE 'phase5-mv-%')");
    execute(
        "DELETE FROM security.item_assignment WHERE item_id IN "
            + "(SELECT id FROM security.item WHERE qr_code LIKE 'phase5-mv-%')");
    execute("DELETE FROM security.item WHERE qr_code LIKE 'phase5-mv-%'");
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
