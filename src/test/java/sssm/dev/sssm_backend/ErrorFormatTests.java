package sssm.dev.sssm_backend;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.Filter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import sssm.dev.sssm_backend.a03_authentication.auth.service.JwtService;

/** Vérifie que toutes les erreurs HTTP utilisent l'enveloppe ItemError. */
@SpringBootTest
class ErrorFormatTests {

  @Autowired private WebApplicationContext context;
  @Autowired private JwtService jwtService;

  private MockMvc mockMvc;
  private String adminToken;
  private String userToken;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
            .build();
    adminToken = jwtService.createToken("test-admin", "error-format-admin", List.of("ADMIN"));
    userToken = jwtService.createToken("test-user", "error-format-user", List.of("USER"));
  }

  @Test
  void missingTokenReturns401ItemError() throws Exception {
    mockMvc
        .perform(get("/api/agency"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errors.message").value(notNullValue()))
        .andExpect(jsonPath("$.errors.code").value("UNAUTHORIZED"));
  }

  @Test
  void invalidTokenReturns401ItemError() throws Exception {
    mockMvc
        .perform(get("/api/agency").header("Authorization", "Bearer not-a-jwt"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errors.message").value(notNullValue()))
        .andExpect(jsonPath("$.errors.code").value("UNAUTHORIZED"));
  }

  @Test
  void missingAdminRoleReturns403ItemError() throws Exception {
    mockMvc
        .perform(get("/api/users").header("Authorization", "Bearer " + userToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errors.message").value(notNullValue()))
        .andExpect(jsonPath("$.errors.code").value("FORBIDDEN"));
  }

  @Test
  void malformedUuidReturns400ItemError() throws Exception {
    mockMvc
        .perform(get("/api/agency/not-a-uuid").header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.message").value(notNullValue()))
        .andExpect(jsonPath("$.errors.code").value("BAD_REQUEST"));
  }

  @Test
  void unknownAgencyUpdateReturns404ItemError() throws Exception {
    mockMvc
        .perform(
            patch("/api/agency/00000000-0000-0000-0000-000000000000")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errors.message").value(notNullValue()))
        .andExpect(jsonPath("$.errors.code").value("NOT_FOUND"));
  }

  @Test
  void malformedJsonReturns400ItemError() throws Exception {
    mockMvc
        .perform(
            post("/api/agency")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.message").value(notNullValue()))
        .andExpect(jsonPath("$.errors.code").value("INVALID_REQUEST_BODY"));
  }

  @Test
  void validationReturns400ItemError() throws Exception {
    mockMvc
        .perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.message").value("Validation échouée"))
        .andExpect(jsonPath("$.errors.detail").value(containsString("login")))
        .andExpect(jsonPath("$.errors.code").value("VALIDATION_ERROR"));
  }

  @Test
  void unknownRouteReturns404ItemError() throws Exception {
    mockMvc
        .perform(get("/api/unknown-route").header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errors.message").value(notNullValue()))
        .andExpect(jsonPath("$.errors.code").value("NOT_FOUND"));
  }

  @Test
  void unsupportedMethodReturns405ItemError() throws Exception {
    mockMvc
        .perform(put("/api/agency").header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.errors.message").value(notNullValue()))
        .andExpect(jsonPath("$.errors.code").value("METHOD_NOT_ALLOWED"));
  }

  @Test
  void unsupportedMediaTypeReturns415ItemError() throws Exception {
    mockMvc
        .perform(
            post("/api/agency")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.TEXT_PLAIN)
                .content("not-json"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.errors.message").value(notNullValue()))
        .andExpect(jsonPath("$.errors.code").value("UNSUPPORTED_MEDIA_TYPE"));
  }
}
