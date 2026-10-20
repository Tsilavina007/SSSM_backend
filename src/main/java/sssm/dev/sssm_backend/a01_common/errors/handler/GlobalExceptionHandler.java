package sssm.dev.sssm_backend.a01_common.errors.handler;

import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
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
      details.add(error.getField() + ": " + error.getDefaultMessage());
    }
    for (ObjectError error : exception.getBindingResult().getGlobalErrors()) {
      details.add(error.getDefaultMessage());
    }

    ItemError error =
        new ItemError(
            "Validation échouée",
            details.isEmpty() ? exception.getMessage() : String.join("; ", details),
            "VALIDATION_ERROR");
    return ResponseEntity.badRequest().body(Map.of("errors", error));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Map<String, ItemError>> handleConstraintViolation(
      ConstraintViolationException exception) {
    String detail =
        exception.getConstraintViolations().stream()
            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
            .collect(Collectors.joining("; "));

    ItemError error =
        new ItemError(
            "Validation échouée",
            detail.isBlank() ? exception.getMessage() : detail,
            "VALIDATION_ERROR");
    return ResponseEntity.badRequest().body(Map.of("errors", error));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<Map<String, ItemError>> handleUnreadableBody(
      HttpMessageNotReadableException exception) {
    ItemError error =
        new ItemError(
            "Corps de requête illisible",
            exception.getMostSpecificCause().getMessage(),
            "INVALID_REQUEST_BODY");
    return ResponseEntity.badRequest().body(Map.of("errors", error));
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<Map<String, ItemError>> handleMissingParameter(
      MissingServletRequestParameterException exception) {
    ItemError error =
        new ItemError("Paramètre obligatoire manquant", exception.getMessage(), "BAD_REQUEST");
    return ResponseEntity.badRequest().body(Map.of("errors", error));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<Map<String, ItemError>> handleTypeMismatch(
      MethodArgumentTypeMismatchException exception) {
    ItemError error = new ItemError("Paramètre invalide", exception.getMessage(), "BAD_REQUEST");
    return ResponseEntity.badRequest().body(Map.of("errors", error));
  }

  @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
  public ResponseEntity<Map<String, ItemError>> handleNoResource(Exception exception) {
    ItemError error = new ItemError("Ressource introuvable", exception.getMessage(), "NOT_FOUND");
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("errors", error));
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<Map<String, ItemError>> handleMethodNotAllowed(
      HttpRequestMethodNotSupportedException exception) {
    ItemError error =
        new ItemError("Méthode non autorisée", exception.getMessage(), "METHOD_NOT_ALLOWED");
    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(Map.of("errors", error));
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<Map<String, ItemError>> handleUnsupportedMediaType(
      HttpMediaTypeNotSupportedException exception) {
    ItemError error =
        new ItemError(
            "Type de média non supporté", exception.getMessage(), "UNSUPPORTED_MEDIA_TYPE");
    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(Map.of("errors", error));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Map<String, ItemError>> handleAccessDenied(
      AccessDeniedException exception) {
    ItemError error = new ItemError("Accès refusé", "Permissions insuffisantes", "FORBIDDEN");
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("errors", error));
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

  private ResponseEntity<Map<String, ItemError>> build(HttpStatus status, Throwable exception) {
    return ResponseEntity.status(status).body(Map.of("errors", ErrorMapper.toItemError(exception)));
  }
}
