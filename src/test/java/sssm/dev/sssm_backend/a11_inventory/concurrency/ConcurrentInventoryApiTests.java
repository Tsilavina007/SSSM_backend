package sssm.dev.sssm_backend.a11_inventory.concurrency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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

import sssm.dev.sssm_backend.a03_authentication.auth.service.JwtService;
import sssm.dev.sssm_backend.support.InventoryTestFixtures;

/**
 * Concurrence sur l'inventaire (Lot 8).
 *
 * <p>Deux requêtes simultanées visent le même article. Quel que soit l'ordonnancement, une seule
 * doit aboutir : le solde ne peut pas être décrémenté deux fois et une seconde affectation active ne
 * peut pas exister. Les tests s'appuient sur les garanties actuelles (index unique partiel et
 * condition {@code quantity + delta >= 0}), sans introduire d'idempotence HTTP.
 */
@SpringBootTest
class ConcurrentInventoryApiTests {

  @Autowired private WebApplicationContext context;
  @Autowired private JwtService jwtService;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private DataSource dataSource;

  private MockMvc mockMvc;
  private InventoryTestFixtures fixtures;
  private String token;
  private ExecutorService executor;

  @BeforeEach
  void setUp() throws Exception {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .addFilters(context.getBean("springSecurityFilterChain", jakarta.servlet.Filter.class))
            .build();
    fixtures = InventoryTestFixtures.create(dataSource, jwtService, passwordEncoder, "LOT8-CONC");
    token = fixtures.adminToken();
    executor = Executors.newFixedThreadPool(2);
  }

  @AfterEach
  void tearDown() throws Exception {
    if (executor != null) {
      executor.shutdownNow();
      executor = null;
    }
    if (fixtures != null) {
      fixtures.close();
      fixtures = null;
    }
  }

  @Test
  @DisplayName("Deux affectations simultanées : une seule réussit et le solde ne passe jamais sous zéro")
  void concurrentAssignmentsLeaveASingleActiveAssignment() throws Exception {
    String itemId = fixtures.insertAvailableItem("CONC-ASG");
    fixtures.setStock("1");

    List<Integer> statuses =
        runConcurrently(
            () ->
                mockMvc
                    .perform(
                        post("/api/v1/item-assignments")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(assignmentBody(itemId)))
                    .andReturn()
                    .getResponse()
                    .getStatus());

    assertEquals(1, statuses.stream().filter(code -> code == 201).count(), statuses.toString());
    assertTrue(
        statuses.stream().allMatch(code -> code == 201 || code == 409),
        "statuts observés : " + statuses);
    assertEquals(1, fixtures.activeAssignments(itemId));
    assertEquals("ASSIGNED", fixtures.itemStatus(itemId));
    assertDecimal("0", fixtures.stock());
  }

  @Test
  @DisplayName("Deux sorties simultanées : un seul mouvement est écrit et le solde reste cohérent")
  void concurrentOutMovementsWriteASingleMovement() throws Exception {
    String itemId = fixtures.insertAvailableItem("CONC-OUT");
    fixtures.setStock("1");

    List<Integer> statuses =
        runConcurrently(
            () ->
                mockMvc
                    .perform(
                        post("/api/v1/inventory/movements")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(outBody(itemId)))
                    .andReturn()
                    .getResponse()
                    .getStatus());

    assertEquals(1, statuses.stream().filter(code -> code == 201).count(), statuses.toString());
    assertTrue(
        statuses.stream().anyMatch(code -> code >= 400 && code < 500),
        "la requête perdante doit être un refus client : " + statuses);
    assertEquals(1, fixtures.movementCount(itemId));
    assertEquals(1, fixtures.activeAssignments(itemId));
    assertEquals("ASSIGNED", fixtures.itemStatus(itemId));
    assertDecimal("0", fixtures.stock());
  }

  @Test
  @DisplayName("Deux retours simultanés : un seul crédite le stock, l'autre est refusé")
  void concurrentReturnsCreditTheStockOnce() throws Exception {
    String itemId = fixtures.insertAvailableItem("CONC-RET");
    fixtures.setStock("1");
    createAssignment(itemId);
    String assignmentId = fixtures.activeAssignmentId(itemId);

    List<Integer> statuses =
        runConcurrently(
            () ->
                mockMvc
                    .perform(
                        post("/api/v1/item-assignments/" + assignmentId + "/return")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andReturn()
                    .getResponse()
                    .getStatus());

    assertEquals(1, statuses.stream().filter(code -> code == 200).count(), statuses.toString());
    assertEquals(1, statuses.stream().filter(code -> code == 409).count(), statuses.toString());
    assertDecimal("1", fixtures.stock());
    assertEquals(0, fixtures.activeAssignments(itemId));
    assertEquals("AVAILABLE", fixtures.itemStatus(itemId));
  }

  /** Lance deux appels après une barrière commune et renvoie leurs statuts HTTP. */
  private List<Integer> runConcurrently(Callable<Integer> request) throws Exception {
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Integer>> futures = new ArrayList<>();
    for (int index = 0; index < 2; index++) {
      futures.add(
          executor.submit(
              () -> {
                ready.countDown();
                start.await();
                try {
                  return request.call();
                } catch (Exception e) {
                  throw new IllegalStateException("requête concurrente en échec", e);
                }
              }));
    }
    assertTrue(ready.await(10, TimeUnit.SECONDS), "les deux requêtes doivent être prêtes");
    start.countDown();

    List<Integer> statuses = new ArrayList<>();
    for (Future<Integer> future : futures) {
      statuses.add(future.get(30, TimeUnit.SECONDS));
    }
    return statuses;
  }

  private void createAssignment(String itemId) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/item-assignments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(assignmentBody(itemId)))
        .andExpect(status().isCreated());
  }

  private String assignmentBody(String itemId) {
    return "{\"item_id\":\""
        + itemId
        + "\",\"employee_id\":\""
        + fixtures.employeeId()
        + "\",\"post_id\":\""
        + fixtures.postId()
        + "\"}";
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

  private static void assertDecimal(String expected, BigDecimal actual) {
    assertEquals(0, new BigDecimal(expected).compareTo(actual), "solde attendu " + expected);
  }
}
