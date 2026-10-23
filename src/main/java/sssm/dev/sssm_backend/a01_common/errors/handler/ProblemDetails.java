package sssm.dev.sssm_backend.a01_common.errors.handler;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import sssm.dev.sssm_backend.a01_common.errors.ItemError;

/**
 * Construit les réponses d'erreur au format Problem Details (RFC 9457).
 *
 * <p>Le corps conserve le code applicatif stable ({@code code}), ajoute un identifiant de trace
 * ({@code traceId}) et un horodatage UTC ({@code timestamp}), et expose l'URI de la requête dans
 * {@code instance}. Le type reste {@code about:blank} : aucun catalogue d'URI n'est publié.
 */
public final class ProblemDetails {

  private ProblemDetails() {}

  public static ProblemDetail of(
      HttpStatus status, ItemError error, HttpServletRequest request) {
    return of(status, error.code(), error.message(), error.detail(), request.getRequestURI());
  }

  public static ProblemDetail of(
      HttpStatus status, String code, String title, String detail, String instance) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(title);
    problem.setInstance(URI.create(instance));
    problem.setProperty("code", code);
    problem.setProperty("traceId", UUID.randomUUID().toString());
    problem.setProperty("timestamp", Instant.now().toString());
    return problem;
  }
}
