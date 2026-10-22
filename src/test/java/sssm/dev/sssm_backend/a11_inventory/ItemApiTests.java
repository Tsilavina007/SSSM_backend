package sssm.dev.sssm_backend.a11_inventory;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
  void qrCodeImageReturnsPng() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get("/api/inventory/items/" + itemId + "/qr-code/image")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.IMAGE_PNG))
            .andReturn();

    byte[] body = result.getResponse().getContentAsByteArray();
    assertTrue(body.length > 8, "l'image PNG doit contenir des données");
    assertEquals((byte) 0x89, body[0], "signature PNG attendue");
    assertEquals((byte) 0x50, body[1], "signature PNG attendue");
    assertEquals((byte) 0x4E, body[2], "signature PNG attendue");
    assertEquals((byte) 0x47, body[3], "signature PNG attendue");
  }

  @Test
  void qrCodeImageUnknownReturnsNotFound() throws Exception {
    mockMvc
        .perform(
            get("/api/inventory/items/" + UUID.randomUUID() + "/qr-code/image")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound());
  }

  @Test
  void historyReturnsItemMovements() throws Exception {
    String marker = "phase5-item-history-" + System.nanoTime();
    String id = createItem(marker, "AVAILABLE", warehouseId);

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

    mockMvc
        .perform(
            get("/api/inventory/items/" + id + "/history")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.item_id").value(id))
        .andExpect(jsonPath("$.movements.length()").value(1))
        .andExpect(jsonPath("$.movements[0].item_id").value(id))
        .andExpect(jsonPath("$.movements[0].movement_type").value("IN"))
        .andExpect(jsonPath("$.assignments").isArray());
  }

  @Test
  void createWithoutQrCodeGeneratesOne() throws Exception {
    String marker = "phase5-autoqr-" + System.nanoTime();
    String body =
        "{\"company_id\":\""
            + companyId
            + "\",\"item_type_id\":\""
            + itemTypeId
            + "\",\"description\":\""
            + marker
            + "\"}";

    mockMvc
        .perform(
            post("/api/inventory/items")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.qr_code").value(org.hamcrest.Matchers.startsWith("QR-")));
  }

  @Test
  void listCombinesWarehouseAndStatusFilters() throws Exception {
    String availableMarker = "phase5-item-filter-a-" + System.nanoTime();
    String retiredMarker = "phase5-item-filter-r-" + System.nanoTime();
    String availableId = createItem(availableMarker, "AVAILABLE", warehouseId);
    String retiredId = createItem(retiredMarker, "RETIRED", warehouseId);

    MvcResult result =
        mockMvc
            .perform(
                get("/api/inventory/items")
                    .param("warehouseId", warehouseId)
                    .param("status", "AVAILABLE")
                    .param("pageSize", "500")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();

    List<String> ids = JsonPath.read(result.getResponse().getContentAsString(), "$[*].id");
    assertTrue(ids.contains(availableId), "l'article AVAILABLE doit être retourné");
    assertFalse(ids.contains(retiredId), "l'article RETIRED doit être exclu");

    List<String> statuses =
        JsonPath.read(result.getResponse().getContentAsString(), "$[*].status");
    assertTrue(statuses.stream().allMatch("AVAILABLE"::equals), "tous les statuts filtrés");
  }

  @Test
  void createUpdateDeleteLifecycle() throws Exception {
    String marker = "phase5-item-" + System.nanoTime();
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
            .andExpect(status().isCreated())
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
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/inventory/items/" + id).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.archived_at").value(notNullValue()));

    mockMvc
        .perform(
            get("/api/inventory/items")
                .param("search", marker)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));

    mockMvc
        .perform(
            post("/api/inventory/items/" + id + "/restore")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.archived_at").doesNotExist());

    mockMvc
        .perform(get("/api/inventory/items/" + id).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.archived_at").doesNotExist());
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
  void deleteUnknownReturnsNotFound() throws Exception {
    mockMvc
        .perform(
            delete("/api/inventory/items/" + UUID.randomUUID())
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errors.code").value("NOT_FOUND"));
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
  void createWithoutOptionalRelationsSucceeds() throws Exception {
    String marker = "phase5-item-npe-" + System.nanoTime();
    String body =
        "{\"company_id\":\""
            + companyId
            + "\",\"item_type_id\":\""
            + itemTypeId
            + "\",\"qr_code\":\""
            + marker
            + "\"}";

    mockMvc
        .perform(
            post("/api/inventory/items")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(notNullValue()))
        .andExpect(jsonPath("$.category_id").doesNotExist())
        .andExpect(jsonPath("$.current_warehouse_id").doesNotExist());
  }

  private String createItem(String qrCode, String status, String warehouse) throws Exception {
    String body =
        "{\"company_id\":\""
            + companyId
            + "\",\"item_type_id\":\""
            + itemTypeId
            + "\",\"current_warehouse_id\":\""
            + warehouse
            + "\",\"qr_code\":\""
            + qrCode
            + "\",\"description\":\"cree par test phase5\",\"status\":\""
            + status
            + "\"}";

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

  private void purge() throws Exception {
    execute(
        "DELETE FROM security.stock_movement WHERE item_id IN ("
            + "SELECT id FROM security.item WHERE qr_code LIKE 'phase5-item-%'"
            + " OR qr_code LIKE 'phase5-bulk-%' OR description LIKE 'phase5-autoqr-%')");
    execute(
        "DELETE FROM security.item_assignment WHERE item_id IN ("
            + "SELECT id FROM security.item WHERE qr_code LIKE 'phase5-item-%'"
            + " OR qr_code LIKE 'phase5-bulk-%' OR description LIKE 'phase5-autoqr-%')");
    execute(
        "DELETE FROM security.item WHERE qr_code LIKE 'phase5-item-%'"
            + " OR qr_code LIKE 'phase5-bulk-%' OR description LIKE 'phase5-autoqr-%'");
    // Les articles et mouvements de test ont modifié le solde calculé : le recalculer
    // depuis l'état réel des articles restants pour ne pas polluer les autres suites.
    if (itemTypeId != null) {
      execute(
          "UPDATE security.stock_balance b SET quantity = COALESCE(("
              + "SELECT count(*) FROM security.item i WHERE i.company_id = b.company_id "
              + "AND i.current_warehouse_id = b.warehouse_id "
              + "AND i.item_type_id = b.item_type_id AND i.status = 'AVAILABLE' "
              + "AND i.archived_at IS NULL), 0), version = b.version + 1, updated_at = now() "
              + "WHERE b.item_type_id = '"
              + itemTypeId
              + "'");
      execute(
          "DELETE FROM security.stock_balance WHERE item_type_id = '"
              + itemTypeId
              + "' AND quantity = 0");
    }
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
