package sssm.dev.sssm_backend.a01_common.errors.mapper;

import java.sql.SQLException;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import sssm.dev.sssm_backend.a00_infrastructure.exception.ClientException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ConflictException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.TooManyRequestsException;
import sssm.dev.sssm_backend.a01_common.errors.ItemError;

@Component
public class ErrorMapper {

  public static ItemError toItemError(Throwable exception) {
    SQLException sqlCause = findSqlCause(exception);
    if (sqlCause != null) {
      return fromSql(sqlCause);
    }

    if (exception instanceof ClientException clientException) {
      String message =
          clientException.getCause() == null
              ? textOr(clientException.getMessage(), "Requête invalide")
              : "Requête invalide";
      return new ItemError(
          message,
          textOr(
              clientException.detail(),
              "La requête n'a pas pu être traitée. Vérifiez les données envoyées."),
          textOr(clientException.code(), "CLIENT_ERROR"));
    }

    if (exception instanceof ConflictException conflictException) {
      return new ItemError(
          textOr(conflictException.getMessage(), "Conflit de ressource"),
          textOr(
              conflictException.detail(),
              "La ressource a été modifiée ou se trouve dans un état incompatible avec cette opération."),
          textOr(conflictException.code(), "CONFLICT"));
    }

    if (exception instanceof TooManyRequestsException tooManyRequestsException) {
      return new ItemError(
          textOr(tooManyRequestsException.getMessage(), "Limite de débit atteinte"),
          textOr(
              tooManyRequestsException.detail(),
              "Trop de requêtes ont été envoyées. Réessayez dans un instant."),
          textOr(tooManyRequestsException.code(), "RATE_LIMITED"));
    }

    if (exception instanceof NotFoundException notFoundException) {      return new ItemError(
          textOr(notFoundException.getMessage(), "Ressource introuvable"),
          textOr(
              notFoundException.detail(),
              "La ressource demandée n'existe pas ou a été supprimée."),
          textOr(notFoundException.code(), "NOT_FOUND"));
    }

    if (exception instanceof ServerException serverException) {
      String message =
          serverException.getCause() == null
              ? textOr(serverException.getMessage(), "Erreur interne du serveur")
              : "Erreur interne du serveur";
      return new ItemError(
          message,
          textOr(
              serverException.detail(),
              "Une erreur interne est survenue. Contactez l'administrateur si le problème persiste."),
          textOr(serverException.code(), "SERVER_ERROR"));
    }

    if (exception instanceof NoSuchElementException noSuchElementException) {
      return new ItemError(
          "Ressource introuvable",
          noSuchElementDetail(noSuchElementException),
          "NOT_FOUND");
    }

    if (exception instanceof IllegalArgumentException illegalArgumentException) {
      return new ItemError(
          "Paramètre invalide",
          illegalArgumentDetail(illegalArgumentException),
          "BAD_REQUEST");
    }

    return new ItemError(
        "Erreur interne du serveur",
        "Une erreur interne est survenue. Contactez l'administrateur si le problème persiste.",
        "INTERNAL_ERROR");
  }

  /**
   * Détermine le statut HTTP à appliquer à une erreur SQL. Un état inconnu reste une erreur serveur
   * (500) : la base ne doit pas exposer sa structure au client.
   */
  public static HttpStatus httpStatus(Throwable exception) {
    SQLException sqlCause = findSqlCause(exception);
    if (sqlCause == null) {
      return null;
    }

    return switch (normalizeState(sqlCause.getSQLState())) {
      case "23505", "40001", "40P01" -> HttpStatus.CONFLICT;
      case "23503", "23514" -> HttpStatus.UNPROCESSABLE_ENTITY;
      case "23502", "22P02", "22001", "22007", "22008", "22003", "22012", "22018" ->
          HttpStatus.BAD_REQUEST;
      case "08000", "08001", "08003", "08004", "08006", "08007", "08P01" ->
          HttpStatus.SERVICE_UNAVAILABLE;
      default -> HttpStatus.INTERNAL_SERVER_ERROR;
    };
  }

  private static ItemError fromSql(SQLException exception) {
    return switch (normalizeState(exception.getSQLState())) {
      case "23505" ->
          new ItemError(
              "Valeur déjà existante",
              "Une ressource avec ces valeurs existe déjà.",
              "CONFLICT");
      case "23503" ->
          new ItemError(
              "Référence inexistante",
              "Une référence fournie n'existe pas.",
              "UNPROCESSABLE_ENTITY");
      case "23502" ->
          new ItemError(
              "Champ obligatoire manquant",
              "Un champ obligatoire est manquant.",
              "BAD_REQUEST");
      case "23514" ->
          new ItemError(
              "Contrainte de validation non respectée",
              "Les données fournies ne respectent pas une règle de validation.",
              "UNPROCESSABLE_ENTITY");
      case "22P02" ->
          new ItemError(
              "Format de valeur invalide",
              "Une valeur fournie n'a pas le format attendu.",
              "BAD_REQUEST");
      case "22001" ->
          new ItemError(
              "Valeur trop longue",
              "Une valeur fournie dépasse la longueur autorisée.",
              "BAD_REQUEST");
      case "22007", "22008" ->
          new ItemError(
              "Date ou heure invalide",
              "Une date ou une heure fournie est invalide.",
              "BAD_REQUEST");
      case "22003", "22012", "22018" ->
          new ItemError(
              "Valeur numérique invalide",
              "Une valeur numérique fournie est invalide.",
              "BAD_REQUEST");
      case "40001", "40P01" ->
          new ItemError(
              "Conflit de transaction",
              "La ressource a été modifiée simultanément. Réessayez.",
              "CONFLICT");
      case "08000", "08001", "08003", "08004", "08006", "08007", "08P01" ->
          new ItemError(
              "Base de données indisponible",
              "Le service de données est temporairement indisponible.",
              "DATABASE_UNAVAILABLE");
      case "42501" ->
          new ItemError(
              "Accès à la base refusé",
              "Le service ne peut pas accéder à cette ressource.",
              "INTERNAL_ERROR");
      default ->
          new ItemError(
              "Erreur de base de données",
              "La base de données n'a pas pu traiter la requête.",
              "DATABASE_ERROR");
    };
  }

  private static String normalizeState(String state) {
    return state == null ? "" : state;
  }

  private static String noSuchElementDetail(NoSuchElementException exception) {
    String message = exception.getMessage();
    if (message == null || message.isBlank() || "No value present".equals(message)) {
      return "La ressource demandée n'existe pas ou a été supprimée.";
    }
    return message;
  }

  private static String illegalArgumentDetail(IllegalArgumentException exception) {
    String message = exception.getMessage();
    String uuidPrefix = "Invalid UUID string: ";
    if (message != null && message.startsWith(uuidPrefix)) {
      return "La valeur '" + message.substring(uuidPrefix.length()) + "' n'est pas un UUID valide.";
    }
    return textOr(message, "La valeur fournie est invalide.");
  }

  private static SQLException findSqlCause(Throwable exception) {
    Throwable current = exception;
    while (current != null) {
      if (current instanceof SQLException sqlException) {
        return sqlException;
      }
      current = current.getCause();
    }
    return null;
  }

  private static String textOr(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }
}
