package sssm.dev.sssm_backend.a11_inventory.inventory_count;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import sssm.dev.sssm_backend.a03_authentication.auth.service.JwtService;

/**
 * Contrat des comptages et de la réconciliation (Lot 6).
 *
 * <p>Vérifie l'instantané figé à l'ouverture, le refus d'un article hors périmètre, le cloisonnement
 * par entreprise, la clôture idempotente et non vide, les ajustements journalisés, l'annulation et
 * la pagination des lignes.
 */
@SpringBootTest
class InventoryCountApiTests {

  @Autowired private WebApplicationContext context;
  @Autowired private JwtService jwtService;
  @Autowired private DataSource dataSource;

  private MockMvc mockMvc;
  private String token;
  private String companyId;
  private String userId;
  private String warehouseId;
  private String itemTypeId;
  private String otherCompanyId;
  private String otherWarehouseId;
  private String prefix;
  private int referenceCounter;
  private String lastReference;

  @BeforeEach
  void setUp() throws Exception {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .addFilters(context.getBean("springSecurityFilterChain", jakarta.servlet.Filter.class))
            .build();

    prefix = "LOT6-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    referenceCounter = 0;
    companyId = firstValue("SELECT id FROM security.company ORDER BY id LIMIT 1");
    userId = firstValue("SELECT id FROM security.app_user ORDER BY id LIMIT 1");
    itemTypeId =
        firstValue(
            "SELECT id FROM security.item_type WHERE company_id = '" + companyId + "' ORDER BY id LIMIT 1");
    token =
        jwtService.createAccessToken(
            userId, "lot6-tester", companyId, List.of("ADMIN"), List.of(), false);

    warehouseId = UUID.randomUUID().toString();
    execute(
        "INSERT INTO security.warehouse (id, company_id, name, active, created_at, updated_at) "
            + "VALUES (?, ?, ?, true, now(), now())",
        warehouseId,
        companyId,
        "Entrepôt " + prefix);
    execute(
        "INSERT INTO security.stock_balance "
            + "(company_id, warehouse_id, item_type_id, quantity, version, created_at, updated_at) "
            + "VALUES (?, ?, ?, 2, 0, now(), now())",
        companyId,
        warehouseId,
        itemTypeId);
  }

  @AfterEach
  void tearDown() throws Exception {
    execute("DELETE FROM security.stock_movement WHERE reference LIKE ?", prefix + "%");
    execute(
        "DELETE FROM security.inventory_count_scan WHERE inventory_count_id IN "
            + "(SELECT id FROM security.inventory_count WHERE reference LIKE ?)",
        prefix + "%");
    execute(
        "DELETE FROM security.inventory_count_expected WHERE inventory_count_id IN "
            + "(SELECT id FROM security.inventory_count WHERE reference LIKE ?)",
        prefix + "%");
    execute("DELETE FROM security.inventory_count WHERE reference LIKE ?", prefix + "%");
    execute("DELETE FROM security.stock_balance WHERE warehouse_id = ?", warehouseId);
    execute("DELETE FROM security.item WHERE qr_code LIKE ?", prefix + "%");
    execute("DELETE FROM security.warehouse WHERE id = ?", warehouseId);
    if (otherWarehouseId != null) {
      execute("DELETE FROM security.warehouse WHERE id = ?", otherWarehouseId);
      otherWarehouseId = null;
    }
    if (otherCompanyId != null) {
      execute("DELETE FROM security.company WHERE id = ?", otherCompanyId);
      otherCompanyId = null;
    }
  }

  @Test
  @DisplayName("La création fige l'instantané des attendus et ignore les champs serveur")
  void createFreezesSnapshotAndIgnoresServerFields() throws Exception {
    String itemInScope = createItem("A", "AVAILABLE", warehouseId, false);
    String itemElsewhere = createItem("B", "LOST", null, false);
    String archived = createItem("C", "AVAILABLE", warehouseId, true);

    String reference = nextReference();
    mockMvc
        .perform(
            post("/api/v1/inventory/inventory-counts")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"warehouse_id\":\""
                        + warehouseId
                        + "\",\"reference\":\""
                        + reference
                        + "\",\"company_id\":\""
                        + UUID.randomUUID()
                        + "\",\"status\":\"COMPLETED\",\"started_by\":\""
                        + UUID.randomUUID()
                        + "\"}"))
        .andExpect(status().isCreated())
        .andExpect(header().exists("Location"))
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.company_id").value(companyId))
        .andExpect(jsonPath("$.started_by").value(userId))
        .andExpect(jsonPath("$.completed_at").value(org.hamcrest.Matchers.nullValue()));

    String countId = firstValue("SELECT id FROM security.inventory_count WHERE reference = ?", reference);
    assertDecimal(
        "1",
        firstDecimal(
            "SELECT expected_quantity FROM security.inventory_count_expected "
                + "WHERE inventory_count_id = ? AND item_id = ?",
            countId,
            itemInScope));
    assertEquals(
        warehouseId,
        firstValue(
            "SELECT expected_warehouse_id FROM security.inventory_count_expected "
                + "WHERE inventory_count_id = ? AND item_id = ?",
            countId,
            itemInScope));
    assertDecimal(
        "0",
        firstDecimal(
            "SELECT expected_quantity FROM security.inventory_count_expected "
                + "WHERE inventory_count_id = ? AND item_id = ?",
            countId,
            itemElsewhere));
    assertNull(
        firstValue(
            "SELECT item_id FROM security.inventory_count_expected "
                + "WHERE inventory_count_id = ? AND item_id = ?",
            countId,
            archived),
        "un article archivé ne doit pas entrer dans l'instantané");

    // Une référence déjà utilisée par l'entreprise est refusée en 409.
    mockMvc
        .perform(
            post("/api/v1/inventory/inventory-counts")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"warehouse_id\":\""
                        + warehouseId
                        + "\",\"reference\":\""
                        + reference
                        + "\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("COUNT_REFERENCE_EXISTS"));
  }

  @Test
  @DisplayName("Le scan lit l'attendu figé et non l'état courant de l'article")
  void scanUsesTheFrozenExpectedQuantity() throws Exception {
    String itemId = createItem("A", "AVAILABLE", warehouseId, false);
    String countId = openCount();

    // L'article quitte l'entrepôt après l'ouverture : l'attendu figé ne doit pas changer.
    execute("UPDATE security.item SET current_warehouse_id = NULL WHERE id = ?", itemId);

    String body =
        mockMvc
            .perform(
                post("/api/v1/inventory/inventory-counts/" + countId + "/scans")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"item_id\":\"" + itemId + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.expected_warehouse_id").value(warehouseId))
            .andExpect(jsonPath("$.actual_warehouse_id").value(warehouseId))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertEquals(0, decimal(body, "$.expected_quantity").compareTo(BigDecimal.ONE));
    assertEquals(0, decimal(body, "$.counted_quantity").compareTo(BigDecimal.ONE));
    assertEquals(0, decimal(body, "$.difference").signum());
    assertEquals("NONE", JsonPath.read(body, "$.discrepancy_type"));
  }

  @Test
  @DisplayName("Un scan hors du périmètre figé ou de l'entreprise est refusé")
  void scanOutsideTheFrozenScopeOrTheCompanyIsRejected() throws Exception {
    String inScopeItem = createItem("SCOPE", "AVAILABLE", warehouseId, false);
    String countId = openCount();

    // Article créé après l'ouverture : absent de l'instantané.
    String lateItem = createItem("LATE", "AVAILABLE", warehouseId, false);
    mockMvc
        .perform(
            post("/api/v1/inventory/inventory-counts/" + countId + "/scans")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"item_id\":\"" + lateItem + "\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ITEM_OUT_OF_SCOPE"));

    // Article archivé avant l'ouverture : également absent.
    String archivedItem = createItem("ARCH", "AVAILABLE", warehouseId, true);
    mockMvc
        .perform(
            post("/api/v1/inventory/inventory-counts/" + countId + "/scans")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"item_id\":\"" + archivedItem + "\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ITEM_OUT_OF_SCOPE"));

    // Article d'une autre entreprise : refusé de la même manière.
    String foreignItem = createForeignKeyItem("FOREIGN");
    mockMvc
        .perform(
            post("/api/v1/inventory/inventory-counts/" + countId + "/scans")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"item_id\":\"" + foreignItem + "\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ITEM_OUT_OF_SCOPE"));

    // Entrepôt constaté d'une autre entreprise : le scan ne doit pas créditer son solde.
    if (otherCompanyId == null) {
      otherCompanyId = createCompany("Autre " + prefix);
    }
    otherWarehouseId = UUID.randomUUID().toString();
    execute(
        "INSERT INTO security.warehouse (id, company_id, name, active, created_at, updated_at) "
            + "VALUES (?, ?, ?, true, now(), now())",
        otherWarehouseId,
        otherCompanyId,
        "Entrepôt " + prefix + " étranger");
    mockMvc
        .perform(
            post("/api/v1/inventory/inventory-counts/" + countId + "/scans")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"item_id\":\""
                        + inScopeItem
                        + "\",\"actual_warehouse_id\":\""
                        + otherWarehouseId
                        + "\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("CLIENT_ERROR"));
  }

  @Test
  @DisplayName("Un comptage d'une autre entreprise reste inaccessible")
  void countOfAnotherCompanyIsInaccessible() throws Exception {
    otherCompanyId = createCompany("Autre " + prefix);
    String reference = nextReference();
    String countId = UUID.randomUUID().toString();
    execute(
        "INSERT INTO security.inventory_count "
            + "(id, company_id, warehouse_id, reference, status, started_at) "
            + "VALUES (?, ?, ?, ?, 'IN_PROGRESS', now())",
        countId,
        otherCompanyId,
        warehouseId,
        reference);

    mockMvc
        .perform(
            get("/api/v1/inventory/inventory-counts/" + countId)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            get("/api/v1/inventory/inventory-counts/" + countId + "/results")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            post("/api/v1/inventory/inventory-counts/" + countId + "/scans")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"item_id\":\"" + UUID.randomUUID() + "\"}"))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            post("/api/v1/inventory/inventory-counts/" + countId + "/close")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            post("/api/v1/inventory/inventory-counts/" + countId + "/cancel")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Un comptage vide ne peut pas être clôturé")
  void emptyCountCannotBeClosed() throws Exception {
    String countId = openCount();

    mockMvc
        .perform(
            post("/api/v1/inventory/inventory-counts/" + countId + "/close")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("COUNT_EMPTY"));

    mockMvc
        .perform(
            get("/api/v1/inventory/inventory-counts/" + countId)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
  }

  @Test
  @DisplayName("La clôture met à jour les articles, le solde et journalise les ajustements")
  void closeReconcilesStockAndJournalsAdjustments() throws Exception {
    String kept = createItem("KEEP", "AVAILABLE", warehouseId, false);
    String missing = createItem("MISS", "AVAILABLE", warehouseId, false);
    String countId = openCount();
    String reference = lastReference;

    scan(countId, "{\"item_id\":\"" + kept + "\"}");
    scan(countId, "{\"item_id\":\"" + missing + "\",\"counted_quantity\":0}");

    mockMvc
        .perform(
            post("/api/v1/inventory/inventory-counts/" + countId + "/close")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"notes\":\"clôture de test\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.completed_by").value(userId))
        .andExpect(jsonPath("$.completed_at").value(org.hamcrest.Matchers.notNullValue()));

    assertEquals(
        "LOST",
        firstValue("SELECT status FROM security.item WHERE id = ?", missing),
        "un article compté absent devient LOST");
    assertNull(firstValue("SELECT current_warehouse_id FROM security.item WHERE id = ?", missing));
    assertNotNull(
        firstValue("SELECT last_inventory_at FROM security.item WHERE id = ?", missing),
        "last_inventory_at doit être renseigné");
    assertEquals(
        "AVAILABLE",
        firstValue("SELECT status FROM security.item WHERE id = ?", kept),
        "un article compté présent reste disponible");
    assertEquals(
        warehouseId, firstValue("SELECT current_warehouse_id FROM security.item WHERE id = ?", kept));

    assertEquals(
        1,
        firstInt("SELECT count(*) FROM security.stock_movement WHERE reference = ?", reference),
        "un seul écart non nul doit produire un mouvement");
    assertEquals(
        "ADJUSTMENT",
        firstValue(
            "SELECT movement_type FROM security.stock_movement WHERE reference = ?", reference));
    assertEquals(
        missing,
        firstValue("SELECT item_id FROM security.stock_movement WHERE reference = ?", reference));
    assertEquals(
        warehouseId,
        firstValue(
            "SELECT source_warehouse_id FROM security.stock_movement WHERE reference = ?",
            reference));
    assertDecimal(
        "1",
        firstDecimal("SELECT quantity FROM security.stock_movement WHERE reference = ?", reference));

    assertDecimal(
        "1",
        firstDecimal(
            "SELECT quantity FROM security.stock_balance WHERE warehouse_id = ? AND item_type_id = ?",
            warehouseId,
            itemTypeId));
  }

  @Test
  @DisplayName("La clôture est idempotente")
  void closeIsIdempotent() throws Exception {
    String missing = createItem("MISS", "AVAILABLE", warehouseId, false);
    String countId = openCount();
    String reference = lastReference;
    scan(countId, "{\"item_id\":\"" + missing + "\",\"counted_quantity\":0}");

    String first = close(countId);
    String firstCompletedAt = JsonPath.read(first, "$.completed_at");
    String second = close(countId);
    String secondCompletedAt = JsonPath.read(second, "$.completed_at");

    assertEquals(
        "COMPLETED", JsonPath.read(second, "$.status"), "la seconde clôture renvoie l'état courant");
    assertEquals(firstCompletedAt, secondCompletedAt, "completed_at ne doit pas être réécrit");
    assertEquals(
        1,
        firstInt("SELECT count(*) FROM security.stock_movement WHERE reference = ?", reference),
        "la seconde clôture ne doit pas doubler l'ajustement");
  }

  @Test
  @DisplayName("L'annulation ne touche ni les soldes ni les articles")
  void cancelLeavesStockUntouched() throws Exception {
    String itemId = createItem("CANCEL", "AVAILABLE", warehouseId, false);
    String countId = openCount();
    String reference = lastReference;
    scan(countId, "{\"item_id\":\"" + itemId + "\"}");

    mockMvc
        .perform(
            post("/api/v1/inventory/inventory-counts/" + countId + "/cancel")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));

    mockMvc
        .perform(
            post("/api/v1/inventory/inventory-counts/" + countId + "/cancel")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));

    mockMvc
        .perform(
            post("/api/v1/inventory/inventory-counts/" + countId + "/close")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("COUNT_CANCELLED"));

    assertEquals(
        "AVAILABLE", firstValue("SELECT status FROM security.item WHERE id = ?", itemId));
    assertEquals(
        warehouseId, firstValue("SELECT current_warehouse_id FROM security.item WHERE id = ?", itemId));
    assertEquals(
        0,
        firstInt("SELECT count(*) FROM security.stock_movement WHERE reference = ?", reference),
        "une annulation n'écrit aucun mouvement");
    assertDecimal(
        "2",
        firstDecimal(
            "SELECT quantity FROM security.stock_balance WHERE warehouse_id = ? AND item_type_id = ?",
            warehouseId,
            itemTypeId));
  }

  @Test
  @DisplayName("Les lignes du comptage sont paginées avec le total")
  void resultsArePaginatedWithTotalCount() throws Exception {
    String first = createItem("Q1", "AVAILABLE", warehouseId, false);
    String second = createItem("Q2", "AVAILABLE", warehouseId, false);
    String third = createItem("Q3", "AVAILABLE", warehouseId, false);
    String countId = openCount();
    scan(countId, "{\"item_id\":\"" + first + "\"}");
    scan(countId, "{\"item_id\":\"" + second + "\"}");
    scan(countId, "{\"item_id\":\"" + third + "\"}");
    mockMvc
        .perform(
            get("/api/v1/inventory/inventory-counts/" + countId + "/results?page=1&pageSize=2")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Total-Count", "3"))
        .andExpect(jsonPath("$.items.length()").value(2))
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.pageSize").value(2))
        .andExpect(jsonPath("$.total").value(3))
        .andExpect(jsonPath("$.totalPages").value(2));
    mockMvc
        .perform(
            get("/api/v1/inventory/inventory-counts/" + countId + "/results?page=2&pageSize=2")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Total-Count", "3"))
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.page").value(2));
  }

  @Test
  @DisplayName("Un comptage inconnu renvoie 404")
  void unknownCountReturnsNotFound() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/inventory/inventory-counts/" + UUID.randomUUID())
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound());
  }

  // --- helpers -----------------------------------------------------------

  private String nextReference() {
    referenceCounter++;
    lastReference = prefix + "-" + referenceCounter;
    return lastReference;
  }

  private String openCount() throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/v1/inventory/inventory-counts")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"warehouse_id\":\""
                            + warehouseId
                            + "\",\"reference\":\""
                            + nextReference()
                            + "\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.id");
  }

  private void scan(String countId, String body) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/inventory/inventory-counts/" + countId + "/scans")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk());
  }

  private String close(String countId) throws Exception {
    return mockMvc
        .perform(
            post("/api/v1/inventory/inventory-counts/" + countId + "/close")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private String createItem(String suffix, String status, String location, boolean archived)
      throws Exception {
    String itemId = UUID.randomUUID().toString();
    execute(
        "INSERT INTO security.item "
            + "(id, company_id, item_type_id, qr_code, status, current_warehouse_id, created_at, "
            + "updated_at, archived_at) VALUES (?, ?, ?, ?, CAST(? AS security.item_status), ?, "
            + "now(), now(), ?)",
        itemId,
        companyId,
        itemTypeId,
        prefix + "-" + suffix,
        status,
        location,
        archived ? java.sql.Timestamp.from(java.time.Instant.now()) : null);
    return itemId;
  }

  private String createForeignKeyItem(String suffix) throws Exception {
    if (otherCompanyId == null) {
      otherCompanyId = createCompany("Autre " + prefix);
    }
    String itemId = UUID.randomUUID().toString();
    execute(
        "INSERT INTO security.item "
            + "(id, company_id, item_type_id, qr_code, status, created_at, updated_at) "
            + "VALUES (?, ?, ?, ?, 'AVAILABLE', now(), now())",
        itemId,
        otherCompanyId,
        itemTypeId,
        prefix + "-" + suffix);
    return itemId;
  }

  private String createCompany(String name) throws Exception {
    String id = UUID.randomUUID().toString();
    execute(
        "INSERT INTO security.company (id, name, country, city, email, phone, created_at, updated_at) "
            + "VALUES (?, ?, 'MG', 'Antananarivo', 'lot6@test.local', '000', now(), now())",
        id,
        name);
    return id;
  }

  private BigDecimal decimal(String json, String path) {
    Object value = JsonPath.read(json, path);
    return new BigDecimal(String.valueOf(value));
  }

  private static void assertDecimal(String expected, BigDecimal actual) {
    assertNotNull(actual, "valeur décimale absente");
    assertEquals(0, new BigDecimal(expected).compareTo(actual), () -> "attendu " + expected + ", obtenu " + actual);
  }

  private void execute(String sql, Object... parameters) throws Exception {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      for (int i = 0; i < parameters.length; i++) {
        statement.setObject(i + 1, bindable(parameters[i]));
      }
      statement.executeUpdate();
    }
  }

  private String firstValue(String sql, Object... parameters) throws Exception {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      for (int i = 0; i < parameters.length; i++) {
        statement.setObject(i + 1, bindable(parameters[i]));
      }
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next() ? resultSet.getString(1) : null;
      }
    }
  }

  /** Convertit les chaînes UUID pour que PostgreSQL compare des types identiques. */
  private static Object bindable(Object value) {
    if (value instanceof String text
        && text.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
      return UUID.fromString(text);
    }
    return value;
  }

  private BigDecimal firstDecimal(String sql, Object... parameters) throws Exception {
    String value = firstValue(sql, parameters);
    return value == null ? null : new BigDecimal(value);
  }

  private int firstInt(String sql, Object... parameters) throws Exception {
    String value = firstValue(sql, parameters);
    return value == null ? 0 : Integer.parseInt(value);
  }
}
