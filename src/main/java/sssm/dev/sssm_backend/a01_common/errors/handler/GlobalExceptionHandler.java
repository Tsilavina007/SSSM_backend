package sssm.dev.sssm_backend.a01_common.errors.handler;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
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
import sssm.dev.sssm_backend.a00_infrastructure.exception.ConflictException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.TooManyRequestsException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.UnauthorizedException;
import sssm.dev.sssm_backend.a01_common.errors.ItemError;
import sssm.dev.sssm_backend.a01_common.errors.mapper.ErrorMapper;

/**
 * Transforme toutes les exceptions non capturées par les contrôleurs en Problem Details (RFC 9457),
 * avec le code applicatif stable, un identifiant de trace et l'URI de la requête.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ClientException.class)
  public ResponseEntity<ProblemDetail> handleClient(
      ClientException exception, HttpServletRequest request) {
    return build(HttpStatus.BAD_REQUEST, exception, request);
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ProblemDetail> handleNotFound(
      NotFoundException exception, HttpServletRequest request) {
    return build(HttpStatus.NOT_FOUND, exception, request);
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ProblemDetail> handleConflict(
      ConflictException exception, HttpServletRequest request) {
    return build(HttpStatus.CONFLICT, exception, request);
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ProblemDetail> handleUnauthorized(
      UnauthorizedException exception, HttpServletRequest request) {
    return problem(
        HttpStatus.UNAUTHORIZED,
        new ItemError(
            exception.getMessage(),
            exception.detail() == null ? exception.getMessage() : exception.detail(),
            "UNAUTHORIZED"),
        request);
  }

  @ExceptionHandler(ServerException.class)
  public ResponseEntity<ProblemDetail> handleServer(
      ServerException exception, HttpServletRequest request) {
    return build(HttpStatus.INTERNAL_SERVER_ERROR, exception, request);
  }

  @ExceptionHandler(TooManyRequestsException.class)
  public ResponseEntity<ProblemDetail> handleTooManyRequests(
      TooManyRequestsException exception, HttpServletRequest request) {
    return build(HttpStatus.TOO_MANY_REQUESTS, exception, request);
  }

  @ExceptionHandler(BindException.class)
  public ResponseEntity<ProblemDetail> handleValidation(
      BindException exception, HttpServletRequest request) {
    List<Map<String, String>> errors = new ArrayList<>();
    List<String> details = new ArrayList<>();
    for (FieldError error : exception.getBindingResult().getFieldErrors()) {
      String message = textOr(error.getDefaultMessage(), "valeur invalide");
      details.add(error.getField() + " : " + message);
      errors.add(fieldError(error.getField(), message));
    }
    for (ObjectError error : exception.getBindingResult().getGlobalErrors()) {
      String message = textOr(error.getDefaultMessage(), "contrainte globale non respectée");
      details.add(message);
      errors.add(fieldError(error.getObjectName(), message));
    }

    String detail =
        details.isEmpty()
            ? "La requête ne respecte pas les contraintes de validation."
            : String.join(" ; ", details);
    ProblemDetail problem =
        ProblemDetails.of(
            HttpStatus.BAD_REQUEST,
            new ItemError("Validation échouée", detail, "VALIDATION_ERROR"),
            request);
    problem.setProperty("errors", errors);
    return respond(HttpStatus.BAD_REQUEST, problem);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ProblemDetail> handleConstraintViolation(
      ConstraintViolationException exception, HttpServletRequest request) {
    List<Map<String, String>> errors = new ArrayList<>();
    List<String> details = new ArrayList<>();
    exception
        .getConstraintViolations()
        .forEach(
            violation -> {
              String field = String.valueOf(violation.getPropertyPath());
              String message = textOr(violation.getMessage(), "valeur invalide");
              details.add(field + " : " + message);
              errors.add(fieldError(field, message));
            });

    if (details.isEmpty()) {
      details.add("La requête ne respecte pas les contraintes de validation.");
    }
    ProblemDetail problem =
        ProblemDetails.of(
            HttpStatus.BAD_REQUEST,
            new ItemError("Validation échouée", String.join(" ; ", details), "VALIDATION_ERROR"),
            request);
    problem.setProperty("errors", errors);
    return respond(HttpStatus.BAD_REQUEST, problem);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ProblemDetail> handleUnreadableBody(
      HttpMessageNotReadableException exception, HttpServletRequest request) {
    String cause = exception.getMostSpecificCause().getMessage();
    String detail =
        textOr(cause, "Le corps de la requête est absent ou mal formé.")
            + " Vérifiez la syntaxe JSON et les types des champs.";
    return problem(
        HttpStatus.BAD_REQUEST,
        new ItemError("Corps de requête illisible", detail, "INVALID_REQUEST_BODY"),
        request);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ProblemDetail> handleMissingParameter(
      MissingServletRequestParameterException exception, HttpServletRequest request) {
    String type =
        exception.getParameterType() == null || exception.getParameterType().isBlank()
            ? ""
            : " de type " + exception.getParameterType();
    String detail =
        "Le paramètre '" + exception.getParameterName() + "'" + type + " est obligatoire.";
    return problem(
        HttpStatus.BAD_REQUEST,
        new ItemError("Paramètre obligatoire manquant", detail, "BAD_REQUEST"),
        request);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ProblemDetail> handleTypeMismatch(
      MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
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
    return problem(
        HttpStatus.BAD_REQUEST, new ItemError("Paramètre invalide", detail, "BAD_REQUEST"), request);
  }

  @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
  public ResponseEntity<ProblemDetail> handleNoResource(HttpServletRequest request) {
    String detail = "Aucune route ne correspond à l'URL " + originalUri(request) + ".";
    return problem(
        HttpStatus.NOT_FOUND, new ItemError("Route introuvable", detail, "NOT_FOUND"), request);
  }

  @SuppressWarnings("null")
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ProblemDetail> handleMethodNotAllowed(
      HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
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
    return problem(
        HttpStatus.METHOD_NOT_ALLOWED,
        new ItemError("Méthode HTTP non autorisée", detail, "METHOD_NOT_ALLOWED"),
        request);
  }

  @SuppressWarnings("null")
  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ProblemDetail> handleUnsupportedMediaType(
      HttpMediaTypeNotSupportedException exception, HttpServletRequest request) {
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
    return problem(
        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
        new ItemError("Type de média non supporté", detail, "UNSUPPORTED_MEDIA_TYPE"),
        request);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ProblemDetail> handleAccessDenied(
      AccessDeniedException exception, HttpServletRequest request) {
    return problem(
        HttpStatus.FORBIDDEN,
        new ItemError(
            "Accès refusé",
            "Vous n'avez pas les permissions nécessaires pour accéder à cette ressource.",
            "FORBIDDEN"),
        request);
  }

  @ExceptionHandler(io.jsonwebtoken.JwtException.class)
  public ResponseEntity<ProblemDetail> handleJwt(
      io.jsonwebtoken.JwtException exception, HttpServletRequest request) {
    return problem(
        HttpStatus.UNAUTHORIZED,
        new ItemError(
            "Jeton invalide",
            "Le jeton JWT est invalide, mal formé ou a expiré.",
            "UNAUTHORIZED"),
        request);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ProblemDetail> handleIllegalArgument(
      IllegalArgumentException exception, HttpServletRequest request) {
    return build(HttpStatus.BAD_REQUEST, exception, request);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleUnexpected(
      Exception exception, HttpServletRequest request) {
    return build(HttpStatus.INTERNAL_SERVER_ERROR, exception, request);
  }

  private static ResponseEntity<ProblemDetail> build(
      HttpStatus status, Throwable exception, HttpServletRequest request) {
    HttpStatus sqlStatus = ErrorMapper.httpStatus(exception);
    HttpStatus effectiveStatus = sqlStatus == null ? status : sqlStatus;
    return problem(effectiveStatus, ErrorMapper.toItemError(exception), request);
  }

  private static ResponseEntity<ProblemDetail> problem(
      HttpStatus status, ItemError error, HttpServletRequest request) {
    return respond(status, ProblemDetails.of(status, error, request));
  }

  private static ResponseEntity<ProblemDetail> respond(
      HttpStatus status, ProblemDetail problem) {
    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  private static Map<String, String> fieldError(String field, String message) {
    Map<String, String> value = new LinkedHashMap<>();
    value.put("field", field);
    value.put("message", message);
    return value;
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
