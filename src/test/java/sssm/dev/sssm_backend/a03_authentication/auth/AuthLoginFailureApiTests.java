package sssm.dev.sssm_backend.a03_authentication.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
 * Échecs d'authentification (Lot 8).
 *
 * <p>Un mot de passe incorrect et un compte inactif ne doivent délivrer aucun jeton. Le statut exact
 * n'est pas figé ici : l'alignement 400/401 du login est suivi séparément (critère {@code A-02} du
 * rapport 15), et ce test doit rester vert lorsqu'il sera corrigé.
 */
@SpringBootTest
class AuthLoginFailureApiTests {

  private static final String PASSWORD = "Lot8-Test-Password!";

  @Autowired private WebApplicationContext context;
  @Autowired private JwtService jwtService;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private DataSource dataSource;

  private MockMvc mockMvc;
  private InventoryTestFixtures fixtures;
  private String username;

  @BeforeEach
  void setUp() throws Exception {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .addFilters(context.getBean("springSecurityFilterChain", jakarta.servlet.Filter.class))
            .build();
    fixtures = InventoryTestFixtures.create(dataSource, jwtService, passwordEncoder, "LOT8-AUTH");
    username = "user-" + fixtures.marker().toLowerCase();
  }

  @AfterEach
  void tearDown() throws Exception {
    if (fixtures != null) {
      fixtures.close();
      fixtures = null;
    }
  }

  @Test
  @DisplayName("Un mot de passe incorrect ne délivre aucun jeton")
  void wrongPasswordIssuesNoToken() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(username, "Mauvais-Mot-De-Passe!")))
        .andExpect(status().is4xxClientError())
        .andExpect(jsonPath("$.access_token").doesNotExist())
        .andExpect(jsonPath("$.refresh_token").doesNotExist());

    assertEquals(
        0,
        fixtures.count(
            "SELECT count(*) FROM security.refresh_token WHERE user_id = ?", fixtures.userId()));
  }

  @Test
  @DisplayName("Un compte inactif ne délivre aucun jeton même avec le bon mot de passe")
  void inactiveAccountIssuesNoToken() throws Exception {
    fixtures.update(
        "UPDATE security.app_user SET status = 'INACTIVE' WHERE id = ?", fixtures.userId());

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(username, PASSWORD)))
        .andExpect(status().is4xxClientError())
        .andExpect(jsonPath("$.access_token").doesNotExist())
        .andExpect(jsonPath("$.refresh_token").doesNotExist());

    assertEquals(
        0,
        fixtures.count(
            "SELECT count(*) FROM security.refresh_token WHERE user_id = ?", fixtures.userId()));
  }

  private static String loginBody(String login, String password) {
    return "{\"login\":\"" + login + "\",\"password\":\"" + password + "\"}";
  }
}
