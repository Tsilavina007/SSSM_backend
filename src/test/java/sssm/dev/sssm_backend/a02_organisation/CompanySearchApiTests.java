package sssm.dev.sssm_backend.a02_organisation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.Filter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a03_authentication.auth.service.JwtService;

/** Vérifie que la recherche d'entreprises ne permet pas d'injection SQL. */
@SpringBootTest
class CompanySearchApiTests {

  @Autowired private WebApplicationContext context;
  @Autowired private JwtService jwtService;

  private MockMvc mockMvc;
  private String adminToken;
  private String companyName;

  @BeforeEach
  void setUp() throws Exception {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
            .build();
    adminToken = jwtService.createToken("test-admin", "company-search-admin", List.of("ADMIN"));
    companyName = "company-search-test-" + System.nanoTime();
    cleanup();
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt =
            conn.prepareStatement("INSERT INTO security.company (name) VALUES (?)")) {
      stmt.setString(1, companyName);
      stmt.executeUpdate();
    }
  }

  @AfterEach
  void cleanUp() throws Exception {
    cleanup();
  }

  @Test
  void searchDoesNotAllowSqlInjection() throws Exception {
    mockMvc
        .perform(
            get("/api/company")
                .param("search", "' OR '1'='1")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(0));
  }

  private void cleanup() throws Exception {
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt =
            conn.prepareStatement("DELETE FROM security.company WHERE name = ?")) {
      stmt.setString(1, companyName);
      stmt.executeUpdate();
    }
  }
}
