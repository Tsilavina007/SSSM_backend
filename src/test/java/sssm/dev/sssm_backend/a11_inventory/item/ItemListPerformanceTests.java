package sssm.dev.sssm_backend.a11_inventory.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import sssm.dev.sssm_backend.a03_authentication.auth.service.JwtService;
import sssm.dev.sssm_backend.support.InventoryTestFixtures;

/**
 * Mesure de latence de la liste des articles sur 1 000 lignes (Lot 8).
 *
 * <p>Le test mesure la médiane de dix appels et la compare à un budget volontairement large pour
 * rester stable en CI. Il journalise la médiane et le maximum observés : c'est une mesure de
 * non-régression, pas une preuve de performance de production.
 */
@SpringBootTest
class ItemListPerformanceTests {

  private static final int ITEMS = 1_000;
  private static final int CALLS = 10;
  private static final long MEDIAN_BUDGET_MS = 2_000;

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
    fixtures = InventoryTestFixtures.create(dataSource, jwtService, passwordEncoder, "LOT8-PERF");
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
  @DisplayName("La liste d'articles sur 1 000 lignes reste sous le budget de latence")
  void listingOneThousandItemsStaysWithinBudget() throws Exception {
    fixtures.insertAvailableItems("PERF", ITEMS);

    // Deux appels de chauffe : pool JDBC, plan de requête et cache de classes.
    for (int index = 0; index < 2; index++) {
      listPage();
    }

    long[] durations = new long[CALLS];
    for (int index = 0; index < CALLS; index++) {
      long startedAt = System.nanoTime();
      mockMvc
          .perform(
              get("/api/v1/inventory/items?page=1&pageSize=50")
                  .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.pageSize").value(50))
          .andExpect(jsonPath("$.items.length()").value(50))
          .andExpect(jsonPath("$.total").isNumber());
      durations[index] = (System.nanoTime() - startedAt) / 1_000_000;
    }

    long[] sorted = durations.clone();
    Arrays.sort(sorted);
    long median = sorted[CALLS / 2];
    long maximum = sorted[CALLS - 1];
    System.out.printf(
        "LOT8 perf: %d articles, page de 50, médiane=%d ms, max=%d ms%n",
        ITEMS, median, maximum);

    assertTrue(
        median <= MEDIAN_BUDGET_MS,
        "médiane " + median + " ms au-delà du budget de " + MEDIAN_BUDGET_MS + " ms");
    assertEquals(
        ITEMS,
        fixtures.count("SELECT count(*) FROM security.item WHERE company_id = ?", fixtures.companyId()));
  }

  private void listPage() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/inventory/items?page=1&pageSize=50")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }
}
