package sssm.dev.sssm_backend.a11_inventory.stock_movement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import sssm.dev.sssm_backend.a03_authentication.auth.service.JwtService;
import sssm.dev.sssm_backend.support.InventoryTestFixtures;

/**
 * Cohérence stock / mouvement / affectation (Lot 8).
 *
 * <p>Les tests créent une entreprise et un article autonomes, puis vérifient que le solde suit le
 * mouvement, qu'un solde insuffisant annule l'ensemble de l'écriture et qu'un retour sans
 * affectation est refusé sans effet de bord.
 */
@SpringBootTest
class StockMovementConsistencyApiTests {

  @Autowired private WebApplicationContext context;
  @Autowired private JwtService jwtService;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private DataSource dataSource;

  private MockMvc mockMvc;
  private InventoryTestFixtures fixtures;
  private String token;

  @BeforeEach
  void setUp() throws Exception {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .addFilters(context.getBean("springSecurityFilterChain", jakarta.servlet.Filter.class))
            .build();
    fixtures = InventoryTestFixtures.create(dataSource, jwtService, passwordEncoder, "LOT8-MVT");
    token = fixtures.adminToken();
  }

  @AfterEach
  void tearDown() throws Exception {
    if (fixtures != null) {
      fixtures.close();
      fixtures = null;
    }
  }

  @Test
  @DisplayName("Une sortie décrémente le solde, retire l'article de l'entrepôt et crée l'affectation")
  void outMovementDecrementsTheBalance() throws Exception {
    String itemId = fixtures.insertAvailableItem("OUT");
    fixtures.setStock("1");

    mockMvc
        .perform(
            post("/api/v1/inventory/movements")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(outBody(itemId)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.item_id").value(itemId))
        .andExpect(jsonPath("$.quantity").value(1));

    assertDecimal("0", fixtures.stock());
    assertEquals("ASSIGNED", fixtures.itemStatus(itemId));
    assertNull(fixtures.itemWarehouse(itemId));
    assertEquals(1, fixtures.activeAssignments(itemId));
    assertEquals(1, fixtures.movementCount(itemId));
  }

  @Test
  @DisplayName("Un retour remet l'article dans l'entrepôt source et restaure le solde")
  void returnMovementRestoresTheBalance() throws Exception {
    String itemId = fixtures.insertAvailableItem("BACK");
    fixtures.setStock("1");
    performMovement(outBody(itemId)).andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/api/v1/inventory/movements")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(returnBody(itemId)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.item_id").value(itemId));

    assertDecimal("1", fixtures.stock());
    assertEquals("AVAILABLE", fixtures.itemStatus(itemId));
    assertEquals(fixtures.warehouseId(), fixtures.itemWarehouse(itemId));
    assertEquals(0, fixtures.activeAssignments(itemId));
    assertEquals(2, fixtures.movementCount(itemId));
  }

  @Test
  @DisplayName("Un stock insuffisant annule le mouvement et l'affectation déjà préparés")
  void insufficientStockRollsBackTheWholeMovement() throws Exception {
    String itemId = fixtures.insertAvailableItem("NOSHOCK");
    fixtures.setStock("0");

    mockMvc
        .perform(
            post("/api/v1/inventory/movements")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(outBody(itemId)))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));

    assertDecimal("0", fixtures.stock());
    assertEquals(0, fixtures.movementCount(itemId), "aucun mouvement ne doit rester écrit");
    assertEquals(0, fixtures.activeAssignments(itemId), "aucune affectation ne doit rester écrite");
    assertEquals("AVAILABLE", fixtures.itemStatus(itemId));
    assertEquals(fixtures.warehouseId(), fixtures.itemWarehouse(itemId));

    // Le solde n'est jamais négatif, même après la tentative refusée.
    assertEquals(
        0,
        fixtures.count(
            "SELECT count(*) FROM security.stock_balance WHERE company_id = ? AND quantity < 0",
            fixtures.companyId()));
  }

  @Test
  @DisplayName("Un retour sans affectation active est refusé et n'écrit rien")
  void returnWithoutAssignmentIsRejected() throws Exception {
    String itemId = fixtures.insertAvailableItem("NOASSIGN");
    fixtures.setStock("1");

    mockMvc
        .perform(
            post("/api/v1/inventory/movements")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(returnBody(itemId)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ITEM_NOT_ASSIGNED"));

    assertEquals(0, fixtures.movementCount(itemId));
    assertDecimal("1", fixtures.stock());
    assertEquals("AVAILABLE", fixtures.itemStatus(itemId));
    assertEquals(fixtures.warehouseId(), fixtures.itemWarehouse(itemId));
  }

  @Test
  @DisplayName("Une destruction décrémente le solde et retire l'article du stock disponible")
  void destructionRetiresTheItemAndDecrementsTheStock() throws Exception {
    String itemId = fixtures.insertAvailableItem("DESTROY");
    fixtures.setStock("1");

    mockMvc
        .perform(
            post("/api/v1/inventory/movements")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(destructionBody(itemId)))
        .andExpect(status().isCreated());

    assertDecimal("0", fixtures.stock());
    assertEquals("RETIRED", fixtures.itemStatus(itemId));
    assertNull(fixtures.itemWarehouse(itemId));
    assertEquals(1, fixtures.movementCount(itemId));
  }

  private ResultActions performMovement(String body) throws Exception {
    return mockMvc.perform(
        post("/api/v1/inventory/movements")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  private String outBody(String itemId) {
    return "{\"warehouse_id\":\""
        + fixtures.warehouseId()
        + "\",\"item_id\":\""
        + itemId
        + "\",\"movement_type\":\"OUT\",\"quantity\":1,\"employee_id\":\""
        + fixtures.employeeId()
        + "\",\"post_id\":\""
        + fixtures.postId()
        + "\"}";
  }

  private String returnBody(String itemId) {
    return "{\"warehouse_id\":\""
        + fixtures.warehouseId()
        + "\",\"item_id\":\""
        + itemId
        + "\",\"movement_type\":\"RETURN\",\"quantity\":1}";
  }

  private String destructionBody(String itemId) {
    return "{\"warehouse_id\":\""
        + fixtures.warehouseId()
        + "\",\"item_id\":\""
        + itemId
        + "\",\"movement_type\":\"DESTRUCTION\",\"quantity\":1,\"source_warehouse_id\":\""
        + fixtures.warehouseId()
        + "\"}";
  }

  private static void assertDecimal(String expected, BigDecimal actual) {
    assertEquals(0, new BigDecimal(expected).compareTo(actual), "solde attendu " + expected);
  }
}
