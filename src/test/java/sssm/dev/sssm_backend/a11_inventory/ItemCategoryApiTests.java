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

/** Routes du module item_category : lecture, création, mise à jour, suppression. */
@SpringBootTest
class ItemCategoryApiTests {

  @Autowired private WebApplicationContext context;
  @Autowired private JwtService jwtService;

  private MockMvc mockMvc;
  private String token;
  private String companyId;
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
      categoryId = firstValue(conn, "SELECT id FROM security.item_category ORDER BY id LIMIT 1");
    }
    Assumptions.assumeTrue(
        companyId != null, "base initialisée avec des données de démonstration requise");
    purge();
  }

  @AfterEach
  void cleanUp() throws Exception {
    purge();
  }

  @Test
  void listReturnsCategories() throws Exception {
    MvcResult result =
        mockMvc
            .perform(get("/api/inventory/item-categories").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();
    List<?> rows = JsonPath.read(result.getResponse().getContentAsString(), "$");
    if (!rows.isEmpty()) {
      assertNotNull(((Map<?, ?>) rows.get(0)).get("id"), "chaque ligne porte un id");
    }
  }

  @Test
  void getByIdReturnsCategory() throws Exception {
    Assumptions.assumeTrue(categoryId != null, "catégorie de démonstration requise");
    mockMvc
        .perform(
            get("/api/inventory/item-categories/" + categoryId)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(categoryId));
  }

  @Test
  void createUpdateDeleteLifecycle() throws Exception {
    String marker = "phase5-cat-" + System.nanoTime();
    String body =
        "{\"company_id\":\"" + companyId + "\",\"name\":\"" + marker + "\",\"description\":\"cat test\"}";

    MvcResult created =
        mockMvc
            .perform(
                post("/api/inventory/item-categories")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(notNullValue()))
            .andExpect(jsonPath("$.name").value(marker))
            .andReturn();
    String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
    assertNotNull(id);

    // preuve SQL
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt =
            conn.prepareStatement("SELECT name FROM security.item_category WHERE id = ?")) {
      stmt.setObject(1, UUID.fromString(id));
      try (ResultSet rs = stmt.executeQuery()) {
        assertTrue(rs.next(), "la catégorie créée doit exister en base");
        assertEquals(marker, rs.getString("name"));
      }
    }

    mockMvc
        .perform(
            patch("/api/inventory/item-categories/" + id)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"phase5-modifie\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.description").value("phase5-modifie"));

    mockMvc
        .perform(
            delete("/api/inventory/item-categories/" + id)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            get("/api/inventory/item-categories/" + id).header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound());
  }

  @Test
  void unknownIdReturnsNotFoundEnvelope() throws Exception {
    mockMvc
        .perform(
            get("/api/inventory/item-categories/" + UUID.randomUUID())
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errors.message").value(notNullValue()));
  }

  private static void purge() throws Exception {
    execute("DELETE FROM security.item_category WHERE name LIKE 'phase5-cat-%'");
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
