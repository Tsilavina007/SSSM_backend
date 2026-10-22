package sssm.dev.sssm_backend.a11_inventory.qr_scan;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import sssm.dev.sssm_backend.a03_authentication.auth.service.JwtService;

/**
 * Contrat du scan QR et des étiquettes (Lot 5).
 *
 * <p>Vérifie la résolution scoped à l'entreprise, l'enrichissement de la réponse, le 404 sur code
 * inconnu, le journal des scans, l'immuabilité du code QR, le cache de l'image et la planche
 * d'étiquettes.
 */
@SpringBootTest
class QrScanApiTests {

  @Autowired private WebApplicationContext context;
  @Autowired private JwtService jwtService;
  @Autowired private DataSource dataSource;

  private MockMvc mockMvc;
  private String token;
  private String companyId;
  private String itemId;

  @BeforeEach
  void setUp() throws Exception {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .addFilters(context.getBean("springSecurityFilterChain", jakarta.servlet.Filter.class))
            .build();

    companyId = firstValue("SELECT id FROM security.company ORDER BY id LIMIT 1");
    itemId = firstValue("SELECT id FROM security.item WHERE qr_code = 'QR-ITEM-001' LIMIT 1");
    String userId = firstValue("SELECT id FROM security.app_user ORDER BY id LIMIT 1");
    // Le jeton porte l'entreprise : la résolution QR est scoped à l'appelant.
    token =
        jwtService.createAccessToken(
            userId, "lot5-qr-tester", companyId, java.util.List.of("ADMIN"), java.util.List.of(), false);
    cleanupJournal();
  }

  @Test
  void knownCodeReturnsEnrichedItem() throws Exception {
    mockMvc
        .perform(
            post("/api/inventory/scans/resolve")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"qrCode\":\"QR-ITEM-001\",\"deviceId\":\"terminal-1\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(itemId))
        .andExpect(jsonPath("$.qr_code").value("QR-ITEM-001"))
        .andExpect(jsonPath("$.item_type.name").value(notNullValue()))
        .andExpect(jsonPath("$.category.name").value(notNullValue()))
        .andExpect(jsonPath("$.warehouse.name").value(notNullValue()))
        .andExpect(jsonPath("$.active_assignment.id").value(notNullValue()))
        .andExpect(jsonPath("$.active_assignment.employee.id").value(notNullValue()))
        .andExpect(jsonPath("$.active_assignment.post.name").value(notNullValue()));
  }

  @Test
  void responseDoesNotLeakInternalFields() throws Exception {
    mockMvc
        .perform(
            post("/api/inventory/scans/resolve")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"qrCode\":\"QR-ITEM-001\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.archived_by").doesNotExist())
        .andExpect(jsonPath("$.created_at").doesNotExist())
        .andExpect(jsonPath("$.company_id").doesNotExist());
  }

  @Test
  void unknownCodeReturnsNotFound() throws Exception {
    mockMvc
        .perform(
            post("/api/inventory/scans/resolve")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"qrCode\":\"QR-INCONNU-999\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errors.code").value("NOT_FOUND"));
  }

  @Test
  void codeOfAnotherCompanyIsNotResolved() throws Exception {
    String otherCompanyId = UUID.randomUUID().toString();
    String otherItemId = UUID.randomUUID().toString();
    String otherCode = "QR-AUTRE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    try (Connection connection = dataSource.getConnection()) {
      try (PreparedStatement statement =
          connection.prepareStatement(
              "INSERT INTO security.company (id, name, country, city, email, phone, created_at,"
                  + " updated_at) VALUES (?, ?, 'MG', 'Antananarivo', 'autre@test.local', '000',"
                  + " now(), now())")) {
        statement.setObject(1, UUID.fromString(otherCompanyId));
        statement.setString(2, "Autre société " + otherCompanyId.substring(0, 4));
        statement.executeUpdate();
      }
      String typeId = firstValue("SELECT id FROM security.item_type ORDER BY id LIMIT 1");
      try (PreparedStatement statement =
          connection.prepareStatement(
              "INSERT INTO security.item (id, company_id, item_type_id, qr_code, status, created_at,"
                  + " updated_at) VALUES (?, ?, ?, ?, 'AVAILABLE', now(), now())")) {
        statement.setObject(1, UUID.fromString(otherItemId));
        statement.setObject(2, UUID.fromString(otherCompanyId));
        statement.setObject(3, UUID.fromString(typeId));
        statement.setString(4, otherCode);
        statement.executeUpdate();
      }
    }

    try {
      mockMvc
          .perform(
              post("/api/inventory/scans/resolve")
                  .header("Authorization", "Bearer " + token)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"qrCode\":\"" + otherCode + "\"}"))
          .andExpect(status().isNotFound());
    } finally {
      try (Connection connection = dataSource.getConnection();
          PreparedStatement statement =
              connection.prepareStatement("DELETE FROM security.item WHERE id = ?")) {
        statement.setObject(1, UUID.fromString(otherItemId));
        statement.executeUpdate();
      }
      try (Connection connection = dataSource.getConnection();
          PreparedStatement statement =
              connection.prepareStatement("DELETE FROM security.company WHERE id = ?")) {
        statement.setObject(1, UUID.fromString(otherCompanyId));
        statement.executeUpdate();
      }
    }
  }

  @Test
  void everyScanIsJournalised() throws Exception {
    mockMvc
        .perform(
            post("/api/inventory/scans/resolve")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"qrCode\":\"QR-ITEM-001\",\"deviceId\":\"terminal-9\"}"))
        .andExpect(status().isOk());

    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement =
            connection.prepareStatement(
                "SELECT result, device_id, item_id FROM security.qr_scan "
                    + "WHERE qr_code = 'QR-ITEM-001' ORDER BY scanned_at DESC LIMIT 1");
        ResultSet resultSet = statement.executeQuery()) {
      assertTrue(resultSet.next(), "le scan doit être journalisé");
      assertEquals("RESOLVED", resultSet.getString("result"));
      assertEquals("terminal-9", resultSet.getString("device_id"));
      assertEquals(itemId, resultSet.getString("item_id"));
    }
  }

  @Test
  void unknownScanIsAlsoJournalised() throws Exception {
    mockMvc
        .perform(
            post("/api/inventory/scans/resolve")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"qrCode\":\"QR-JOURNALISE-404\"}"))
        .andExpect(status().isNotFound());

    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement =
            connection.prepareStatement(
                "SELECT result FROM security.qr_scan WHERE qr_code = 'QR-JOURNALISE-404' "
                    + "ORDER BY scanned_at DESC LIMIT 1");
        ResultSet resultSet = statement.executeQuery()) {
      assertTrue(resultSet.next(), "le scan inconnu doit être journalisé");
      assertEquals("NOT_FOUND", resultSet.getString("result"));
    }
  }

  @Test
  void qrCodeCannotBeModifiedAfterCreation() throws Exception {
    mockMvc
        .perform(
            patch("/api/inventory/items/" + itemId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"qr_code\":\"QR-MODIFIE\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.code").value("IMMUTABLE_QR_CODE"));
  }

  @Test
  void qrCodeImageIsCacheable() throws Exception {
    String etag =
        mockMvc
            .perform(
                get("/api/inventory/items/" + itemId + "/qr-code/image")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", containsString("max-age")))
            .andExpect(header().exists("ETag"))
            .andReturn()
            .getResponse()
            .getHeader("ETag");

    mockMvc
        .perform(
            get("/api/inventory/items/" + itemId + "/qr-code/image")
                .header("Authorization", "Bearer " + token)
                .header("If-None-Match", etag))
        .andExpect(status().isNotModified());
  }

  @Test
  void labelSheetContainsTheQrCodeAndReadableText() throws Exception {
    String html =
        mockMvc
            .perform(
                get("/api/inventory/items/" + itemId + "/label")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertTrue(html.contains("data:image/png;base64,"), "le QR code doit être intégré à l'étiquette");
    assertTrue(html.contains("QR-ITEM-001"), "le code doit être lisible");
  }

  @Test
  void batchLabelSheetRendersEveryItem() throws Exception {
    String html =
        mockMvc
            .perform(
                post("/api/inventory/labels/batch")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"item_ids\":[\"" + itemId + "\"],\"size\":150}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertTrue(html.contains("QR-ITEM-001"), "la planche doit contenir le code de l'article");
  }

  @Test
  void labelOfUnknownItemReturnsNotFound() throws Exception {
    mockMvc
        .perform(
            get("/api/inventory/items/" + UUID.randomUUID() + "/label")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound());
  }

  @Test
  void blankQrCodeIsRejected() throws Exception {
    mockMvc
        .perform(
            post("/api/inventory/scans/resolve")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"qrCode\":\"  \"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void uuidLikeQrCodeDoesNotProduceServerError() throws Exception {
    mockMvc
        .perform(
            post("/api/inventory/scans/resolve")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"qrCode\":\"" + UUID.randomUUID() + "\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errors.code").value(not("INTERNAL_ERROR")));
  }

  private String firstValue(String sql) throws Exception {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery()) {
      return resultSet.next() ? resultSet.getString(1) : null;
    }
  }

  private void cleanupJournal() throws Exception {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement =
            connection.prepareStatement(
                "DELETE FROM security.qr_scan WHERE device_id = 'terminal-9' "
                    + "OR qr_code IN ('QR-JOURNALISE-404', 'QR-ITEM-001')")) {
      statement.executeUpdate();
    }
  }
}
