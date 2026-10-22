package sssm.dev.sssm_backend.a04_personnel;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.Filter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
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

/** Vérifie que la création minimale d'un employé laisse PostgreSQL appliquer ses defaults. */
@SpringBootTest
class EmployeeDefaultsTests {

  @Autowired private WebApplicationContext context;
  @Autowired private JwtService jwtService;

  private MockMvc mockMvc;
  private String token;
  private String companyId;

  @BeforeEach
  void setUp() throws Exception {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
            .build();
    token = jwtService.createToken("test-user", "phase2-employee", List.of("ADMIN"));

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt =
            conn.prepareStatement("SELECT id FROM security.company ORDER BY id LIMIT 1");
        ResultSet rs = stmt.executeQuery()) {
      companyId = rs.next() ? rs.getString(1) : null;
    }
    Assumptions.assumeTrue(companyId != null, "entreprise de démonstration requise");
    purge();
  }

  @AfterEach
  void cleanUp() throws Exception {
    purge();
  }

  @Test
  void createWithoutStatusAppliesDatabaseDefault() throws Exception {
    String employeeNumber = "phase2-emp-" + System.nanoTime();
    String body =
        "{\"company_id\":\""
            + companyId
            + "\",\"employee_number\":\""
            + employeeNumber
            + "\",\"first_name\":\"Test\",\"last_name\":\"Defaults\"}";

    mockMvc
        .perform(
            post("/api/employees")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(notNullValue()))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.created_at").value(notNullValue()));

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt =
            conn.prepareStatement(
                "SELECT status FROM security.employee WHERE company_id = ? AND employee_number = ?")) {
      stmt.setObject(1, java.util.UUID.fromString(companyId));
      stmt.setString(2, employeeNumber);
      try (ResultSet rs = stmt.executeQuery()) {
        assertTrue(rs.next(), "l'employé minimal doit être créé");
        assertEquals("ACTIVE", rs.getString("status"));
      }
    }
  }

  private static void purge() throws Exception {
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt =
            conn.prepareStatement(
                "DELETE FROM security.employee WHERE employee_number LIKE 'phase2-emp-%'")) {
      stmt.executeUpdate();
    }
  }
}
