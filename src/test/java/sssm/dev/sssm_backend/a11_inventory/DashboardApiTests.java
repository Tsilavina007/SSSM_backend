package sssm.dev.sssm_backend.a11_inventory;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.Filter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import sssm.dev.sssm_backend.a03_authentication.auth.service.JwtService;

/**
 * Routes du module dashboard : synthèse et alertes de stock bas (lecture seule, réponses en
 * tableaux — un agrégat par entreprise pour la synthèse).
 */
@SpringBootTest
class DashboardApiTests {

  @Autowired private WebApplicationContext context;
  @Autowired private JwtService jwtService;

  private MockMvc mockMvc;
  private String token;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
            .build();
    token = jwtService.createToken("test-user", "phase5-tester", List.of("ADMIN"));
  }

  @Test
  void summaryReturnsArrayOfCounters() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get("/api/inventory/dashboard/summary")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();

    List<?> rows = JsonPath.read(result.getResponse().getContentAsString(), "$");
    if (!rows.isEmpty()) {
      Map<?, ?> first = (Map<?, ?>) rows.get(0);
      assertNotNull(first.get("items"), "compteur d'articles attendu");
      assertNotNull(first.get("warehouses"), "compteur d'entrepôts attendu");
      assertNotNull(first.get("operations"), "compteur d'opérations attendu");
    }
  }

  @Test
  void lowStockReturnsArray() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get("/api/inventory/alerts/low-stock").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();

    List<?> rows = JsonPath.read(result.getResponse().getContentAsString(), "$");
    if (!rows.isEmpty()) {
      Map<?, ?> first = (Map<?, ?>) rows.get(0);
      assertNotNull(first.get("id"), "identifiant de l'article attendu");
      assertNotNull(first.get("stock_quantity"), "quantité en stock attendue");
    }
  }
}
