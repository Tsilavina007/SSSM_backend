package sssm.dev.sssm_backend.a01_common.errors.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.stereotype.Component;

import sssm.dev.sssm_backend.a00_infrastructure.exception.ClientException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;
import sssm.dev.sssm_backend.a01_common.errors.ItemError;

@Component
public class ErrorMapper {

  public static ItemError toItemError(Throwable exception) {
    Throwable sqlCause = findCause(exception, PSQLException.class);
    if (sqlCause instanceof PSQLException sqlException) {
      return fromSql(sqlException);
    }

    if (exception instanceof ClientException clientException) {
      return new ItemError(
          textOr(clientException.getMessage(), "Requête invalide"),
          textOr(
              clientException.detail(),
              "La requête n'a pas pu être traitée. Vérifiez les données envoyées."),
          textOr(clientException.code(), "CLIENT_ERROR"));
    }

    if (exception instanceof NotFoundException notFoundException) {
      return new ItemError(
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
          textOr(serverException.detail(), describeCause(serverException)),
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
        describeCause(exception),
        "INTERNAL_ERROR");
  }

  private static ItemError fromSql(PSQLException exception) {
    ServerErrorMessage serverError = exception.getServerErrorMessage();
    String state = textOr(exception.getSQLState(), "DATABASE_ERROR");
    String postgresMessage =
        serverError != null ? serverError.getMessage() : exception.getMessage();
    String postgresDetail = serverError != null ? serverError.getDetail() : null;

    String detail =
        joinDetails(
            textOr(postgresMessage, "La base de données a rejeté la requête."),
            postgresDetail,
            "SQLState : " + state);

    return new ItemError(sqlMessage(state), detail, state);
  }

  private static String sqlMessage(String state) {
    return switch (state) {
      case "23505" -> "Valeur déjà existante";
      case "23503" -> "Référence inexistante";
      case "23502" -> "Champ obligatoire manquant";
      case "23514" -> "Contrainte de validation non respectée";
      case "22P02" -> "Format de valeur invalide";
      case "22001" -> "Valeur trop longue";
      case "22007", "22008" -> "Date ou heure invalide";
      case "08000", "08003", "08006" -> "Base de données indisponible";
      case "40001", "40P01" -> "Conflit de transaction";
      case "42P01" -> "Table ou vue introuvable";
      case "42703" -> "Colonne introuvable";
      case "42501" -> "Droits insuffisants sur la base de données";
      default -> "Erreur de base de données";
    };
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

  private static String describeCause(Throwable exception) {
    Throwable cause = exception.getCause() == null ? exception : exception.getCause();
    String causeMessage = cause.getMessage();
    String causeType = cause.getClass().getSimpleName();

    if (causeMessage != null && !causeMessage.isBlank()) {
      return causeType + " : " + causeMessage;
    }
    return "Aucun détail technique supplémentaire n'est disponible (" + causeType + ").";
  }

  private static String joinDetails(String... values) {
    List<String> parts = new ArrayList<>();
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        parts.add(value);
      }
    }
    return String.join(" | ", parts);
  }

  private static Throwable findCause(Throwable exception, Class<? extends Throwable> type) {
    Throwable current = exception;
    while (current != null) {
      if (type.isInstance(current)) {
        return current;
      }
      current = current.getCause();
    }
    return null;
  }

  private static String textOr(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }
}
