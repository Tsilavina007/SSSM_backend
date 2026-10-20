package sssm.dev.sssm_backend.a01_common.errors.handler;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sssm.dev.sssm_backend.a01_common.errors.ItemError;

/**
 * Remplace le contrôleur d'erreur Spring Boot par défaut afin que même les
 * erreurs gérées par le conteneur (404, 405, etc.) respectent l'enveloppe
 * ItemError.
 */
@RestController
public class ItemErrorController implements ErrorController {

  @RequestMapping("${server.error.path:${error.path:/error}}")
  public ResponseEntity<Map<String, ItemError>> error(HttpServletRequest request) {
    int status = status(request);
    Throwable exception = exception(request);
    String message = message(request, exception, status);
    String detail =
        exception != null ? exception.getMessage() : request.getRequestURI();

    ItemError error = new ItemError(message, detail, code(status));
    return ResponseEntity.status(status).body(Map.of("errors", error));
  }

  private static int status(HttpServletRequest request) {
    Object value = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
    return value instanceof Integer status ? status : 500;
  }

  private static Throwable exception(HttpServletRequest request) {
    Object value = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
    return value instanceof Throwable exception ? exception : null;
  }

  private static String message(
      HttpServletRequest request, Throwable exception, int status) {
    Object value = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
    if (value instanceof String message && !message.isBlank()) {
      return message;
    }
    if (exception != null && exception.getMessage() != null) {
      return exception.getMessage();
    }
    return switch (status) {
      case 400 -> "Requête invalide";
      case 401 -> "Authentification requise";
      case 403 -> "Accès refusé";
      case 404 -> "Ressource introuvable";
      case 405 -> "Méthode non autorisée";
      case 415 -> "Type de média non supporté";
      default -> status >= 500 ? "Erreur interne du serveur" : "Erreur HTTP " + status;
    };
  }

  private static String code(int status) {
    return switch (status) {
      case 400 -> "BAD_REQUEST";
      case 401 -> "UNAUTHORIZED";
      case 403 -> "FORBIDDEN";
      case 404 -> "NOT_FOUND";
      case 405 -> "METHOD_NOT_ALLOWED";
      case 415 -> "UNSUPPORTED_MEDIA_TYPE";
      default -> status >= 500 ? "INTERNAL_ERROR" : "HTTP_" + status;
    };
  }
}
