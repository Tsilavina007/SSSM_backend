package sssm.dev.sssm_backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import sssm.dev.sssm_backend.a01_common.errors.ItemError;
import sssm.dev.sssm_backend.a01_common.errors.mapper.ErrorMapper;

/** Vérifie la traduction stable des erreurs SQL sans fuite de SQLSTATE ni de message PostgreSQL. */
class SqlErrorMappingTests {

  @Test
  void uniqueViolationMapsToConflict() {
    assertMapped("23505", HttpStatus.CONFLICT, "CONFLICT", "Valeur déjà existante");
  }

  @Test
  void foreignKeyViolationMapsToUnprocessableEntity() {
    assertMapped(
        "23503", HttpStatus.UNPROCESSABLE_ENTITY, "UNPROCESSABLE_ENTITY", "Référence inexistante");
  }

  @Test
  void notNullViolationMapsToBadRequest() {
    assertMapped("23502", HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Champ obligatoire manquant");
  }

  @Test
  void databaseUnavailableMapsToServiceUnavailable() {
    assertMapped(
        "08006",
        HttpStatus.SERVICE_UNAVAILABLE,
        "DATABASE_UNAVAILABLE",
        "Base de données indisponible");
  }

  @Test
  void sqlStateAndPostgresMessageAreNeverExposed() {
    ItemError error =
        ErrorMapper.toItemError(
            new SQLException("password=secret SQLState 23505", "23505"));

    assertEquals("CONFLICT", error.code());
    assertFalse(error.detail().contains("23505"));
    assertFalse(error.detail().contains("SQLState"));
    assertFalse(error.detail().contains("password"));
  }

  private static void assertMapped(
      String sqlState, HttpStatus expectedStatus, String expectedCode, String expectedMessage) {
    SQLException exception = new SQLException("détail interne", sqlState);
    assertEquals(expectedStatus, ErrorMapper.httpStatus(exception));
    ItemError error = ErrorMapper.toItemError(exception);
    assertEquals(expectedCode, error.code());
    assertEquals(expectedMessage, error.message());
    assertFalse(error.detail().contains(sqlState));
  }
}
