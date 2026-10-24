package sssm.dev.sssm_backend.a11_inventory.item_assignment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.hamcrest.Matchers.nullValue;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import sssm.dev.sssm_backend.a01_common.enums.ItemStatus;
import sssm.dev.sssm_backend.a03_authentication.auth.service.JwtService;
import sssm.dev.sssm_backend.support.InventoryTestFixtures;

/**
 * Cycle de vie d'une affectation (Lot 8).
 *
 * <p>Chaque test crée son entreprise et son article : il ne dépend d'aucune donnée de démonstration.
 * Il vérifie qu'une affectation retire l'article du stock, qu'un retour le remet exactement une fois
 * dans son entrepôt d'origine, et qu'un article indisponible ne peut pas être affecté.
 */
@SpringBootTest
class ItemAssignmentLifecycleApiTests {

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
    fixtures = InventoryTestFixtures.create(dataSource, jwtService, passwordEncoder, "LOT8-ASG");
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
  @DisplayName("Une affectation retire l'article du stock et le passe ASSIGNED")
  void assignmentRemovesTheItemFromAvailableStock() throws Exception {
    String itemId = fixtures.insertAvailableItem("ASSIGN");
    fixtures.setStock("1");

    mockMvc
        .perform(
            post("/api/v1/item-assignments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(itemId)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.item_id").value(itemId))
        .andExpect(jsonPath("$.company_id").value(fixtures.companyId()))
        .andExpect(jsonPath("$.assigned_by").value(fixtures.userId()))
        .andExpect(jsonPath("$.returned_at").value(nullValue()));

    assertEquals("ASSIGNED", fixtures.itemStatus(itemId));
    assertNull(fixtures.itemWarehouse(itemId), "un article affecté n'a plus d'entrepôt courant");
    assertEquals(1, fixtures.activeAssignments(itemId));
    assertDecimal("0", fixtures.stock());
  }

  @Test
  @DisplayName("Un retour remet l'article et le stock une seule fois dans l'entrepôt d'origine")
  void returnRestoresTheItemAndTheStockExactlyOnce() throws Exception {
    String itemId = fixtures.insertAvailableItem("RETURN");
    fixtures.setStock("1");
    createAssignment(itemId);
    assertEquals(1, fixtures.activeAssignments(itemId));
    assertDecimal("0", fixtures.stock());

    String assignmentId = fixtures.activeAssignmentId(itemId);
    assertNotNull(assignmentId);

    mockMvc
        .perform(
            post("/api/v1/item-assignments/" + assignmentId + "/return")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"condition_on_return\":\"Bon état\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.returned_at").isNotEmpty())
        .andExpect(jsonPath("$.returned_by").value(fixtures.userId()));

    assertEquals("AVAILABLE", fixtures.itemStatus(itemId));
    assertEquals(fixtures.warehouseId(), fixtures.itemWarehouse(itemId));
    assertEquals(0, fixtures.activeAssignments(itemId));
    assertDecimal("1", fixtures.stock());

    // Un second retour est refusé et ne crédite pas une deuxième fois le solde.
    mockMvc
        .perform(
            post("/api/v1/item-assignments/" + assignmentId + "/return")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ASSIGNMENT_ALREADY_RETURNED"));

    assertDecimal("1", fixtures.stock());
    assertEquals(0, fixtures.activeAssignments(itemId));
  }

  @Test
  @DisplayName("Un article indisponible ne peut pas être affecté et le stock reste intact")
  void unavailableItemCannotBeAssigned() throws Exception {
    String itemId = fixtures.insertItem("LOST", ItemStatus.LOST, null);
    fixtures.setStock("1");

    mockMvc
        .perform(
            post("/api/v1/item-assignments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(itemId)))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("ITEM_NOT_AVAILABLE"));

    assertEquals("LOST", fixtures.itemStatus(itemId));
    assertEquals(0, fixtures.activeAssignments(itemId));
    assertDecimal("1", fixtures.stock());
  }

  @Test
  @DisplayName("Une affectation sur un article inconnu renvoie 404 sans écrire de ligne")
  void unknownItemReturnsNotFoundWithoutSideEffect() throws Exception {
    fixtures.setStock("1");

    mockMvc
        .perform(
            post("/api/v1/item-assignments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(java.util.UUID.randomUUID().toString())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));

    assertEquals(
        0,
        fixtures.count(
            "SELECT count(*) FROM security.item_assignment WHERE company_id = ?",
            fixtures.companyId()));
    assertDecimal("1", fixtures.stock());
  }

  @Test
  @DisplayName("Deux affectations successives du même article sont refusées par le service")
  void twoSequentialAssignmentsAreRejected() throws Exception {
    String itemId = fixtures.insertAvailableItem("DOUBLE");
    fixtures.setStock("1");
    createAssignment(itemId);

    mockMvc
        .perform(
            post("/api/v1/item-assignments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(itemId)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ITEM_NOT_AVAILABLE"));

    assertEquals(1, fixtures.activeAssignments(itemId));
    assertDecimal("0", fixtures.stock());
  }

  private void createAssignment(String itemId) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/item-assignments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(itemId)))
        .andExpect(status().isCreated());
  }

  private String body(String itemId) {
    return "{\"item_id\":\""
        + itemId
        + "\",\"employee_id\":\""
        + fixtures.employeeId()
        + "\",\"post_id\":\""
        + fixtures.postId()
        + "\"}";
  }

  private static void assertDecimal(String expected, BigDecimal actual) {
    assertEquals(0, new BigDecimal(expected).compareTo(actual), "solde attendu " + expected);
  }
}
