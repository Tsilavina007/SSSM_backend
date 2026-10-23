package sssm.dev.sssm_backend.a01_common.http;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import sssm.dev.sssm_backend.a01_common.web.ApiVersionFilter;
import sssm.dev.sssm_backend.a03_authentication.auth.service.JwtService;

/**
 * Contrat HTTP du Lot 7 : versionnement, Problem Details, pagination, Location et dates UTC.
 *
 * <p>Les tests utilisent la base réelle de la suite, comme les autres tests d'intégration.
 */
@SpringBootTest
class HttpContractApiTests {

  private static final String ISO_UTC =
      "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?(Z|[+]00:00)";

  @Autowired private WebApplicationContext context;
  @Autowired private JwtService jwtService;
  @Autowired private DataSource dataSource;

  private MockMvc mockMvc;
  private MockMvc legacyMockMvc;
  private String token;
  private String companyId;
  private String itemTypeId;
  private String itemId;
  private String prefix;

  @BeforeEach
  void setUp() throws Exception {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .addFilters(context.getBean("springSecurityFilterChain", jakarta.servlet.Filter.class))
            .build();
    legacyMockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .addFilters(
                context.getBean(ApiVersionFilter.class),
                context.getBean("springSecurityFilterChain", jakarta.servlet.Filter.class))
            .build();

    prefix = "LOT7-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    companyId = firstValue("SELECT id FROM security.company ORDER BY id LIMIT 1");
    String userId = firstValue("SELECT id FROM security.app_user ORDER BY id LIMIT 1");
    itemTypeId =
        firstValue(
            "SELECT id FROM security.item_type WHERE company_id = '" + companyId + "' LIMIT 1");
    token =
        jwtService.createAccessToken(
            userId, "lot7-tester", companyId, List.of("ADMIN"), List.of(), false);

    itemId = insertItem(prefix + "-HISTORY");
  }

  @AfterEach
  void tearDown() throws Exception {
    execute(
        "DELETE FROM security.stock_movement WHERE item_id IN "
            + "(SELECT id FROM security.item WHERE qr_code LIKE ?)",
        prefix + "%");
    execute(
        "DELETE FROM security.item_assignment WHERE item_id IN "
            + "(SELECT id FROM security.item WHERE qr_code LIKE ?)",
        prefix + "%");
    execute(
        "DELETE FROM security.stock_balance WHERE item_type_id = ? AND warehouse_id IS NULL",
        itemTypeId);
    execute("DELETE FROM security.item WHERE qr_code LIKE ?", prefix + "%");
  }

  @Test
  @DisplayName("La route canonique est /api/v1 et l'alias /api est déprécié")
  void versionedRouteIsCanonicalAndLegacyAliasIsDeprecated() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/inventory/items?page=1&pageSize=1")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(header().doesNotExist("Deprecation"))
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.page").value(1));

    legacyMockMvc
        .perform(
            get("/api/inventory/items?page=1&pageSize=1")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(header().string("Deprecation", "true"))
        .andExpect(
            header().string("Link", containsString("</api/v1/inventory/items>; rel=\"successor-version\"")))
        .andExpect(jsonPath("$.items").isArray());
  }

  @Test
  @DisplayName("Une ressource inconnue renvoie un Problem Details stable")
  void unknownResourceReturnsProblemDetails() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/inventory/items/" + UUID.randomUUID())
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.code").value("NOT_FOUND"))
        .andExpect(jsonPath("$.title").isNotEmpty())
        .andExpect(jsonPath("$.detail").isNotEmpty())
        .andExpect(jsonPath("$.instance").value(containsString("/api/v1/inventory/items/")))
        .andExpect(jsonPath("$.traceId").isNotEmpty())
        .andExpect(jsonPath("$.timestamp", matchesPattern(ISO_UTC)));
  }

  @Test
  @DisplayName("Une validation invalide renvoie problem+json avec les champs en erreur")
  void validationErrorReturnsFieldErrors() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/inventory/items")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors").isArray())
        .andExpect(jsonPath("$.errors[0].field").isNotEmpty())
        .andExpect(jsonPath("$.errors[0].message").isNotEmpty());
  }

  @Test
  @DisplayName("Une création renvoie 201, Location et des dates ISO-8601 UTC")
  void creationReturnsCreatedLocationAndUtcDates() throws Exception {
    String qrCode = prefix + "-CREATED";
    mockMvc
        .perform(
            post("/api/v1/inventory/items")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"company_id\":\""
                        + companyId
                        + "\",\"item_type_id\":\""
                        + itemTypeId
                        + "\",\"qr_code\":\""
                        + qrCode
                        + "\",\"description\":\"Article contrat HTTP\"}"))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", containsString("/api/v1/inventory/items/")))
        .andExpect(jsonPath("$.id").isNotEmpty())
        .andExpect(jsonPath("$.created_at", matchesPattern(ISO_UTC)));

    String createdId = firstValue("SELECT id FROM security.item WHERE qr_code = ?", qrCode);
    mockMvc
        .perform(
            get("/api/v1/inventory/items/" + createdId)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(header().doesNotExist("Location"))
        .andExpect(jsonPath("$.created_at", matchesPattern(ISO_UTC)));
  }

  @Test
  @DisplayName("La pagination expose total et normalise page=0")
  void itemListExposesTotalAndNormalizesPageZero() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/inventory/items?page=0&pageSize=500")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.pageSize").value(500))
        .andExpect(jsonPath("$.total").isNumber())
        .andExpect(jsonPath("$.totalPages").isNumber())
        .andExpect(jsonPath("$.items").isArray());
  }

  @Test
  @DisplayName("L'historique d'un article inconnu renvoie 404 et celui d'un article connu est paginé")
  void historyIsPaginatedAndUnknownItemReturnsNotFound() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/inventory/items/" + UUID.randomUUID() + "/history")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));

    mockMvc
        .perform(
            get("/api/v1/inventory/items/" + itemId + "/history?page=1&pageSize=5")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.item_id").value(itemId))
        .andExpect(jsonPath("$.movements.items").isArray())
        .andExpect(jsonPath("$.movements.total").isNumber())
        .andExpect(jsonPath("$.movements.page").value(1))
        .andExpect(jsonPath("$.movements.pageSize").value(5))
        .andExpect(jsonPath("$.assignments.items").isArray())
        .andExpect(jsonPath("$.assignments.total").isNumber());
  }

  @Test
  @DisplayName("Un filtre de mouvement inconnu renvoie 400 sans SQLSTATE")
  void invalidMovementTypeReturnsBadRequest() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/inventory/items/" + itemId + "/history?movementType=WRONG")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("CLIENT_ERROR"));
  }

  @Test
  @DisplayName("Un UUID invalide renvoie 400 et non 500")
  void invalidUuidReturnsBadRequest() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/inventory/items/not-a-uuid")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        .andExpect(jsonPath("$.traceId").isNotEmpty());
  }

  @Test
  @DisplayName("Une suppression réussie renvoie 204 sans corps")
  void successfulDeleteReturnsNoContent() throws Exception {
    String toDelete = insertItem(prefix + "-DELETE");
    mockMvc
        .perform(
            delete("/api/v1/inventory/items/" + toDelete)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));
  }

  private String insertItem(String qrCode) throws Exception {
    String id = UUID.randomUUID().toString();
    execute(
        "INSERT INTO security.item "
            + "(id, company_id, item_type_id, qr_code, status, created_at, updated_at) "
            + "VALUES (?, ?, ?, ?, 'AVAILABLE', now(), now())",
        id,
        companyId,
        itemTypeId,
        qrCode);
    return id;
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
}
