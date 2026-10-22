package sssm.dev.sssm_backend.a11_inventory;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
 * Routes du module item_type : CRUD, liste par catégorie, et les trois routes « stub »
 * (low-stock, items, stocks) qui répondent 200 sans corps — comportement préexistant
 * conservé tel quel (dette D5).
 */
@SpringBootTest
class ItemTypeApiTests {

  @Autowired private WebApplicationContext context;
  @Autowired private JwtService jwtService;

  private MockMvc mockMvc;
  private String token;
  private String companyId;
  private String itemTypeId;
  private String itemId;
  private String categoryId;

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
      itemId = firstValue(conn, "SELECT id FROM security.item ORDER BY id LIMIT 1");
      categoryId = firstValue(conn, "SELECT id FROM security.item_category ORDER BY id LIMIT 1");
    }
    Assumptions.assumeTrue(
        companyId != null && itemTypeId != null,
        "base initialisée avec des données de démonstration requise");
    purge();
  }

  @AfterEach
  void cleanUp() throws Exception {
    purge();
  }

  @Test
  void listReturnsTypes() throws Exception {
    mockMvc
        .perform(get("/api/inventory/item-types").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(notNullValue()));
  }

  @Test
  void getByIdReturnsType() throws Exception {
    mockMvc
        .perform(
            get("/api/inventory/item-types/" + itemTypeId)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(itemTypeId));
  }

  @Test
  void listByCategoryReturnsArray() throws Exception {
    Assumptions.assumeTrue(categoryId != null, "catégorie de démonstration requise");
    MvcResult result =
        mockMvc
            .perform(
                get("/api/inventory/item-types/category/" + categoryId)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();
    List<?> rows = JsonPath.read(result.getResponse().getContentAsString(), "$");
    if (!rows.isEmpty()) {
      assertNotNull(((Map<?, ?>) rows.get(0)).get("id"), "chaque ligne porte un id");
    }
  }

  @Test
  void createUpdateDeleteLifecycle() throws Exception {
    Assumptions.assumeTrue(categoryId != null, "catégorie de démonstration requise");
    String marker = "phase5-type-" + System.nanoTime();
    String body =
        "{\"company_id\":\""
            + companyId
            + "\",\"category_id\":\""
            + categoryId
            + "\",\"code\":\""
            + marker
            + "\",\"name\":\"type phase5\",\"unit\":\"unite\","
            + "\"quantity_total\":0,\"minimum_stock\":0,\"status\":\"AVAILABLE\"}";

    MvcResult created =
        mockMvc
            .perform(
                post("/api/inventory/item-types")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(notNullValue()))
            .andExpect(jsonPath("$.code").value(marker))
            .andReturn();
    String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
    assertNotNull(id);

    // preuve SQL
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt =
            conn.prepareStatement("SELECT code FROM security.item_type WHERE id = ?")) {
      stmt.setObject(1, UUID.fromString(id));
      try (ResultSet rs = stmt.executeQuery()) {
        assertTrue(rs.next(), "le type créé doit exister en base");
        assertEquals(marker, rs.getString("code"));
      }
    }

    mockMvc
        .perform(
            patch("/api/inventory/item-types/" + id)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"phase5-modifie\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.description").value("phase5-modifie"));

    mockMvc
        .perform(delete("/api/inventory/item-types/" + id).header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/inventory/item-types/" + id).header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound());
  }

  @Test
  void unknownCategoryReturns422WithoutSqlDetails() throws Exception {
    String marker = "phase5-type-fk-" + System.nanoTime();
    String body =
        "{\"company_id\":\""
            + companyId
            + "\",\"category_id\":\""
            + UUID.randomUUID()
            + "\",\"code\":\""
            + marker
            + "\",\"name\":\"type fk inconnue\"}";

    mockMvc
        .perform(
            post("/api/inventory/item-types")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.errors.message").value("Référence inexistante"))
        .andExpect(jsonPath("$.errors.detail").value(not(containsString("SQLState"))))
        .andExpect(jsonPath("$.errors.detail").value(not(containsString("23503"))))
        .andExpect(jsonPath("$.errors.code").value("UNPROCESSABLE_ENTITY"));
  }

  @Test
  void createWithoutOptionalValuesAppliesDatabaseDefaults() throws Exception {
    String marker = "phase5-type-min-" + System.nanoTime();
    String body =
        "{\"company_id\":\""
            + companyId
            + "\",\"code\":\""
            + marker
            + "\",\"name\":\"type minimal\"}";

    MvcResult created =
        mockMvc
            .perform(
                post("/api/inventory/item-types")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(notNullValue()))
            .andExpect(jsonPath("$.unit").value("piece"))
            .andExpect(jsonPath("$.quantity_total").value(0))
            .andExpect(jsonPath("$.minimum_stock").value(0))
            .andExpect(jsonPath("$.status").value("AVAILABLE"))
            .andExpect(jsonPath("$.created_at").value(notNullValue()))
            .andReturn();

    String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
    assertNotNull(id);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt =
            conn.prepareStatement(
                "SELECT unit, quantity_total, minimum_stock, status FROM security.item_type"
                    + " WHERE id = ?")) {
      stmt.setObject(1, UUID.fromString(id));
      try (ResultSet rs = stmt.executeQuery()) {
        assertTrue(rs.next(), "le type minimal doit être créé");
        assertEquals("piece", rs.getString("unit"));
        assertEquals(0, rs.getBigDecimal("quantity_total").intValue());
        assertEquals(0, rs.getBigDecimal("minimum_stock").intValue());
        assertEquals("AVAILABLE", rs.getString("status"));
      }
    }
  }

  @Test
  void lowStockStubReturnsEmptyArray() throws Exception {
    mockMvc
        .perform(get("/api/inventory/item-types/low-stock").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void itemsByTypeStubReturnsEmptyArray() throws Exception {
    Assumptions.assumeTrue(itemId != null, "article de démonstration requis");
    mockMvc
        .perform(
            get("/api/inventory/item-types/items/" + itemId)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void stocksByTypeStubReturnsEmptyArray() throws Exception {
    Assumptions.assumeTrue(itemId != null, "article de démonstration requis");
    mockMvc
        .perform(
            get("/api/inventory/item-types/stocks/" + itemId)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(0));
  }

  private static void purge() throws Exception {
    execute("DELETE FROM security.item_type WHERE code LIKE 'phase5-type-%'");
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
