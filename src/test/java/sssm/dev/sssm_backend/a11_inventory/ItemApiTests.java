package sssm.dev.sssm_backend.a11_inventory;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
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

/** Routes du module item : lecture, création, mise à jour, suppression, QR code, bulk. */
@SpringBootTest
class ItemApiTests {

  @Autowired private WebApplicationContext context;
  @Autowired private JwtService jwtService;

  private MockMvc mockMvc;
  private String token;
  private String companyId;
  private String itemTypeId;
  private String categoryId;
  private String warehouseId;
  private String itemId;
  private String qrCode;

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
      categoryId = firstValue(conn, "SELECT id FROM security.item_category ORDER BY id LIMIT 1");
      warehouseId = firstValue(conn, "SELECT id FROM security.warehouse ORDER BY id LIMIT 1");
      itemId = firstValue(conn, "SELECT id FROM security.item ORDER BY id LIMIT 1");
      qrCode = firstValue(conn, "SELECT qr_code FROM security.item ORDER BY id LIMIT 1");
    }
    Assumptions.assumeTrue(
        companyId != null
            && itemTypeId != null
            && categoryId != null
            && warehouseId != null
            && itemId != null
            && qrCode != null,
        "base initialisée avec des données de démonstration requise");
    purge();
  }

  @AfterEach
  void cleanUp() throws Exception {
    purge();
  }

  @Test
  void listReturnsItems() throws Exception {
    mockMvc
        .perform(get("/api/inventory/items").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(notNullValue()));
  }

  @Test
  void listWithoutTokenIsRejected() throws Exception {
    // toute route /api/** exige un JWT — 401 ou 403 selon la stratégie de la chaîne
    mockMvc.perform(get("/api/inventory/items")).andExpect(status().is4xxClientError());
  }

  @Test
  void getByIdReturnsItem() throws Exception {
    mockMvc
        .perform(get("/api/inventory/items/" + itemId).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(itemId));
  }

  @Test
  void getByIdUnknownReturnsNotFoundEnvelope() throws Exception {
    mockMvc
        .perform(
            get("/api/inventory/items/" + UUID.randomUUID())
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errors.message").value(notNullValue()));
  }

  @Test
  void qrCodeEndpointReturnsCode() throws Exception {
    mockMvc
        .perform(
            get("/api/inventory/items/" + itemId + "/qr-code")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.qr_code").value(qrCode));
  }

  @Test
  void qrCodeEndpointUnknownReturnsNotFound() throws Exception {
    mockMvc
        .perform(
            get("/api/inventory/items/" + UUID.randomUUID() + "/qr-code")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound());
  }

  @Test
  void createUpdateDeleteLifecycle() throws Exception {
    String marker = "phase5-item-" + System.nanoTime();
    // category_id et current_warehouse_id DOIVENT être fournis : le mapper de réponse
    // famille A n'a pas de null-check (voir createWithoutOptionalFailsWithMapperNpe)
    String body =
        "{\"company_id\":\""
            + companyId
            + "\",\"item_type_id\":\""
            + itemTypeId
            + "\",\"category_id\":\""
            + categoryId
            + "\",\"current_warehouse_id\":\""
            + warehouseId
            + "\",\"qr_code\":\""
            + marker
            + "\",\"description\":\"cree par test phase5\",\"status\":\"AVAILABLE\"}";

    MvcResult created =
        mockMvc
            .perform(
                post("/api/inventory/items")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(notNullValue()))
            .andExpect(jsonPath("$.qr_code").value(marker))
            .andExpect(jsonPath("$.status").value("AVAILABLE"))
            .andReturn();
    String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
    assertNotNull(id);

    // preuve SQL : la ligne existe bien dans security.item
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt =
            conn.prepareStatement("SELECT description FROM security.item WHERE id = ?")) {
      stmt.setObject(1, UUID.fromString(id));
      try (ResultSet rs = stmt.executeQuery()) {
        assertTrue(rs.next(), "l'article créé doit exister en base");
        assertEquals("cree par test phase5", rs.getString("description"));
      }
    }

    mockMvc
        .perform(get("/api/inventory/items/" + id).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id));

    mockMvc
        .perform(
            patch("/api/inventory/items/" + id)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"phase5-modifie\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.description").value("phase5-modifie"));

    mockMvc
        .perform(delete("/api/inventory/items/" + id).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value("Item deleted : " + id));

    mockMvc
        .perform(get("/api/inventory/items/" + id).header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound());
  }

  @Test
  void patchUnknownReturnsNotFound() throws Exception {
    mockMvc
        .perform(
            patch("/api/inventory/items/" + UUID.randomUUID())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"x\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void deleteUnknownReturnsOkWithout404() throws Exception {
    // fidélité contrôleur : la suppression ne vérifie pas l'existence
    mockMvc
        .perform(
            delete("/api/inventory/items/" + UUID.randomUUID())
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }

  @Test
  void bulkPartiallySucceeds() throws Exception {
    String okMarker = "phase5-bulk-ok-" + System.nanoTime();
    String badMarker = "phase5-bulk-bad-" + System.nanoTime();
    String shared =
        "\",\"category_id\":\""
            + categoryId
            + "\",\"current_warehouse_id\":\""
            + warehouseId
            + "\",\"qr_code\":\"";
    String body =
        "[{\"company_id\":\""
            + companyId
            + "\",\"item_type_id\":\""
            + itemTypeId
            + shared
            + okMarker
            + "\"},"
            // item_type_id manquant : NOT NULL sans défaut → échec isolé (ServerException absorbée)
            + "{\"company_id\":\""
            + companyId
            + shared
            + badMarker
            + "\",\"description\":\"sans type\"}]";

    mockMvc
        .perform(
            post("/api/inventory/items/bulk")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.createdItems.length()").value(1))
        .andExpect(jsonPath("$.createdItems[0].qr_code").value(okMarker))
        .andExpect(jsonPath("$.failedItems.length()").value(1))
        .andExpect(jsonPath("$.failedItems[0].item.qr_code").value(badMarker))
        .andExpect(jsonPath("$.failedItems[0].errors.message").value(notNullValue()));
  }

  @Test
  void createWithoutOptionalFailsWithMapperNpe() {
    // DETTE PRÉEXISTANTE (découverte par ces tests, module item = référence seule) :
    // ItemMapper.toRest() appelle UUID.fromString() sans null-check sur category_id ET
    // current_warehouse_id (lignes 35-39) → NPE non enveloppée → 500 brut, alors que ces
    // colonnes sont facultatives en base. Même défaut côté ItemTypeMapper (category_id) et
    // WarehouseMapper (branch_id). Si un null-check est ajouté, supprimer ce test.
    String marker = "phase5-item-npe-" + System.nanoTime();
    String body =
        "{\"company_id\":\""
            + companyId
            + "\",\"item_type_id\":\""
            + itemTypeId
            + "\",\"qr_code\":\""
            + marker
            + "\"}";

    assertThrows(
        ServletException.class,
        () ->
            mockMvc.perform(
                post("/api/inventory/items")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)),
        "le mapper de réponse plante sur une FK optionnelle omise (dette connue)");
  }

  private static void purge() throws Exception {
    execute(
        "DELETE FROM security.item WHERE qr_code LIKE 'phase5-item-%'"
            + " OR qr_code LIKE 'phase5-bulk-%'");
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
