package sssm.dev.sssm_backend.a11_inventory;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.Filter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a03_authentication.auth.service.JwtService;

/** Route unique du module qr_scan : POST /api/inventory/scans/resolve (lecture seule). */
@SpringBootTest
class QrScanApiTests {

  @Autowired private WebApplicationContext context;
  @Autowired private JwtService jwtService;

  private MockMvc mockMvc;
  private String token;
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
      itemId = firstValue(conn, "SELECT id FROM security.item ORDER BY id LIMIT 1");
      qrCode = firstValue(conn, "SELECT qr_code FROM security.item ORDER BY id LIMIT 1");
    }
    Assumptions.assumeTrue(
        itemId != null && qrCode != null,
        "base initialisée avec des données de démonstration requise");
  }

  @Test
  void resolveKnownQrCodeReturnsItem() throws Exception {
    mockMvc
        .perform(
            post("/api/inventory/scans/resolve")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"qrCode\":\"" + qrCode + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(notNullValue()))
        .andExpect(jsonPath("$[0].qr_code").value(qrCode))
        .andExpect(jsonPath("$[0].id").value(notNullValue()));
  }

  @Test
  void resolveUnknownQrCodeReturnsEmptyArrayIn200() throws Exception {
    mockMvc
        .perform(
            post("/api/inventory/scans/resolve")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"qrCode\":\"phase5-nowhere-" + System.nanoTime() + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void malformedBodyReturnsBadRequest() throws Exception {
    // un scalaire est coercible en String par Jackson (ex. 123 → "123" → 200 sans
    // correspondance) ; c'est un objet imbriqué qui casse la désérialisation du record → 400
    mockMvc
        .perform(
            post("/api/inventory/scans/resolve")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"qrCode\":{\"bad\":true}}"))
        .andExpect(status().isBadRequest());
  }

  private static String firstValue(Connection conn, String sql) throws Exception {
    try (PreparedStatement stmt = conn.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()) {
      return rs.next() ? rs.getString(1) : null;
    }
  }
}
