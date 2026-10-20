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
 * ItemError avec un message explicite.
 */
@RestController
public class ItemErrorController implements ErrorController {

  @RequestMapping("${server.error.path:${error.path:/error}}")
  public ResponseEntity<Map<String, ItemError>> error(HttpServletRequest request) {
    int status = status(request);
    ItemError error = new ItemError(message(status), detail(request, status), code(status));
    return ResponseEntity.status(status).body(Map.of("errors", error));
  }

  private static int status(HttpServletRequest request) {
    Object value = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
    return value instanceof Integer status ? status : 500;
  }

  private static String message(int status) {
    return switch (status) {
      case 400 -> "Requête invalide";
      case 401 -> "Authentification requise";
      case 403 -> "Accès refusé";
      case 404 -> "Route introuvable";
      case 405 -> "Méthode HTTP non autorisée";
      case 415 -> "Type de média non supporté";
      case 500 -> "Erreur interne du serveur";
      default -> "Erreur HTTP " + status;
    };
  }

  private static String detail(HttpServletRequest request, int status) {
    String original = attribute(request, RequestDispatcher.ERROR_MESSAGE);
    Throwable exception = exception(request);
    String cause = exception == null ? null : exception.getMessage();

    return switch (status) {
      case 400 -> textOr(original, textOr(cause, "La requête est invalide ou mal formée."));
      case 401 -> "Un jeton JWT valide est requis pour accéder à cette ressource.";
      case 403 -> "Vous n'avez pas les permissions nécessaires pour accéder à cette ressource.";
      case 404 -> "Aucune route ne correspond à l'URL " + originalUri(request) + ".";
      case 405 -> textOr(original, "La méthode HTTP utilisée n'est pas autorisée pour cette route.");
      case 415 -> textOr(original, "Le type de média de la requête n'est pas supporté.");
      case 500 ->
          textOr(
              cause,
              textOr(
                  original,
                  "Une erreur inattendue est survenue. Consultez les journaux du serveur."));
      default ->
          textOr(
              original,
              textOr(cause, "La requête a échoué avec le statut HTTP " + status + "."));
    };
  }

  private static Throwable exception(HttpServletRequest request) {
    Object value = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
    return value instanceof Throwable exception ? exception : null;
  }

  private static String originalUri(HttpServletRequest request) {
    String uri = attribute(request, RequestDispatcher.ERROR_REQUEST_URI);
    return uri == null ? request.getRequestURI() : uri;
  }

  private static String attribute(HttpServletRequest request, String name) {
    Object value = request.getAttribute(name);
    return value instanceof String text && !text.isBlank() ? text : null;
  }

  private static String textOr(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
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
