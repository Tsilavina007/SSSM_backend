package sssm.dev.sssm_backend.a01_common.errors.handler;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import sssm.dev.sssm_backend.a00_infrastructure.exception.ClientException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;
import sssm.dev.sssm_backend.a01_common.errors.ItemError;
import sssm.dev.sssm_backend.a01_common.errors.mapper.ErrorMapper;

/**
 * Transforme toutes les exceptions non capturées par les contrôleurs en une enveloppe unique :
 * {"errors": {"message", "detail", "code"}}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ClientException.class)
  public ResponseEntity<Map<String, ItemError>> handleClient(ClientException exception) {
    return build(HttpStatus.BAD_REQUEST, exception);
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<Map<String, ItemError>> handleNotFound(NotFoundException exception) {
    return build(HttpStatus.NOT_FOUND, exception);
  }

  @ExceptionHandler(ServerException.class)
  public ResponseEntity<Map<String, ItemError>> handleServer(ServerException exception) {
    return build(HttpStatus.INTERNAL_SERVER_ERROR, exception);
  }

  @ExceptionHandler(BindException.class)
  public ResponseEntity<Map<String, ItemError>> handleValidation(BindException exception) {
    List<String> details = new ArrayList<>();
    for (FieldError error : exception.getBindingResult().getFieldErrors()) {
      details.add(
          error.getField()
              + " : "
              + textOr(error.getDefaultMessage(), "valeur invalide"));
    }
    for (ObjectError error : exception.getBindingResult().getGlobalErrors()) {
      details.add(textOr(error.getDefaultMessage(), "contrainte globale non respectée"));
    }

    String detail =
        details.isEmpty()
            ? "La requête ne respecte pas les contraintes de validation."
            : String.join(" ; ", details);
    return badRequest(new ItemError("Validation échouée", detail, "VALIDATION_ERROR"));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Map<String, ItemError>> handleConstraintViolation(
      ConstraintViolationException exception) {
    String detail =
        exception.getConstraintViolations().stream()
            .map(
                violation ->
                    violation.getPropertyPath() + " : " + violation.getMessage())
            .collect(Collectors.joining(" ; "));

    if (detail.isBlank()) {
      detail = "La requête ne respecte pas les contraintes de validation.";
    }
    return badRequest(new ItemError("Validation échouée", detail, "VALIDATION_ERROR"));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<Map<String, ItemError>> handleUnreadableBody(
      HttpMessageNotReadableException exception) {
    String cause = exception.getMostSpecificCause().getMessage();
    String detail =
        textOr(cause, "Le corps de la requête est absent ou mal formé.")
            + " Vérifiez la syntaxe JSON et les types des champs.";
    return badRequest(new ItemError("Corps de requête illisible", detail, "INVALID_REQUEST_BODY"));
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<Map<String, ItemError>> handleMissingParameter(
      MissingServletRequestParameterException exception) {
    String type =
        exception.getParameterType() == null || exception.getParameterType().isBlank()
            ? ""
            : " de type " + exception.getParameterType();
    String detail =
        "Le paramètre '" + exception.getParameterName() + "'" + type + " est obligatoire.";
    return badRequest(new ItemError("Paramètre obligatoire manquant", detail, "BAD_REQUEST"));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<Map<String, ItemError>> handleTypeMismatch(
      MethodArgumentTypeMismatchException exception) {
    String requiredType =
        exception.getRequiredType() == null
            ? "attendu"
            : exception.getRequiredType().getSimpleName();
    String value = exception.getValue() == null ? "null" : String.valueOf(exception.getValue());
    String detail =
        "Le paramètre '"
            + exception.getName()
            + "' doit être de type "
            + requiredType
            + ". Valeur reçue : '"
            + value
            + "'.";
    return badRequest(new ItemError("Paramètre invalide", detail, "BAD_REQUEST"));
  }

  @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
  public ResponseEntity<Map<String, ItemError>> handleNoResource(HttpServletRequest request) {
    String detail = "Aucune route ne correspond à l'URL " + originalUri(request) + ".";
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(Map.of("errors", new ItemError("Route introuvable", detail, "NOT_FOUND")));
  }

  @SuppressWarnings("null")
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<Map<String, ItemError>> handleMethodNotAllowed(
      HttpRequestMethodNotSupportedException exception) {
    String methods =
        exception.getSupportedHttpMethods() == null
            ? ""
            : exception.getSupportedHttpMethods().stream()
                .map(HttpMethod::name)
                .sorted()
                .collect(Collectors.joining(", "));
    String detail =
        "La méthode "
            + exception.getMethod()
            + " n'est pas autorisée pour cette route."
            + (methods.isBlank() ? "" : " Méthodes autorisées : " + methods + ".");
    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
        .body(
            Map.of(
                "errors",
                new ItemError("Méthode HTTP non autorisée", detail, "METHOD_NOT_ALLOWED")));
  }

  @SuppressWarnings("null")
  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<Map<String, ItemError>> handleUnsupportedMediaType(
      HttpMediaTypeNotSupportedException exception) {
    String supported =
        exception.getSupportedMediaTypes().stream()
            .map(MediaType::toString)
            .collect(Collectors.joining(", "));
    String contentType =
        exception.getContentType() == null ? "inconnu" : exception.getContentType().toString();
    String detail =
        "Le type de média '"
            + contentType
            + "' n'est pas supporté."
            + (supported.isBlank() ? "" : " Types acceptés : " + supported + ".");
    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
        .body(
            Map.of(
                "errors",
                new ItemError(
                    "Type de média non supporté", detail, "UNSUPPORTED_MEDIA_TYPE")));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Map<String, ItemError>> handleAccessDenied(
      AccessDeniedException exception) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(
            Map.of(
                "errors",
                new ItemError(
                    "Accès refusé",
                    "Vous n'avez pas les permissions nécessaires pour accéder à cette ressource.",
                    "FORBIDDEN")));
  }

  @ExceptionHandler(io.jsonwebtoken.JwtException.class)
  public ResponseEntity<Map<String, ItemError>> handleJwt(io.jsonwebtoken.JwtException exception) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(
            Map.of(
                "errors",
                new ItemError(
                    "Jeton invalide",
                    "Le jeton JWT est invalide, mal formé ou a expiré.",
                    "UNAUTHORIZED")));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, ItemError>> handleIllegalArgument(
      IllegalArgumentException exception) {
    return build(HttpStatus.BAD_REQUEST, exception);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, ItemError>> handleUnexpected(Exception exception) {
    return build(HttpStatus.INTERNAL_SERVER_ERROR, exception);
  }

  private static ResponseEntity<Map<String, ItemError>> badRequest(ItemError error) {
    return ResponseEntity.badRequest().body(Map.of("errors", error));
  }

  private static ResponseEntity<Map<String, ItemError>> build(
      HttpStatus status, Throwable exception) {
    return ResponseEntity.status(status).body(Map.of("errors", ErrorMapper.toItemError(exception)));
  }

  private static String originalUri(HttpServletRequest request) {
    Object value = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
    if (value instanceof String uri && !uri.isBlank()) {
      return uri;
    }
    return request.getRequestURI();
  }

  private static String textOr(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }
}
