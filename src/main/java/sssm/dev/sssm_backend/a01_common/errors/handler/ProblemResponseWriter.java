package sssm.dev.sssm_backend.a01_common.errors.handler;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import tools.jackson.databind.ObjectMapper;

import sssm.dev.sssm_backend.a01_common.errors.ItemError;

/**
 * Écrit une réponse Problem Details depuis un filtre de servlet, là où le
 * {@code @RestControllerAdvice} n'intervient pas (sécurité, JWT, erreurs du conteneur).
 */
public final class ProblemResponseWriter {

  private ProblemResponseWriter() {}

  public static void write(
      HttpServletResponse response,
      ObjectMapper objectMapper,
      int status,
      String code,
      String title,
      String detail,
      String instance)
      throws IOException {
    ProblemDetail problem =
        ProblemDetails.of(HttpStatus.valueOf(status), code, title, detail, instance);
    write(response, objectMapper, status, problem);
  }

  public static void write(
      HttpServletResponse response,
      ObjectMapper objectMapper,
      int status,
      ItemError error,
      String instance)
      throws IOException {
    write(
        response,
        objectMapper,
        status,
        error.code(),
        error.message(),
        error.detail(),
        instance);
  }

  public static void write(
      HttpServletResponse response,
      ObjectMapper objectMapper,
      int status,
      ProblemDetail problem)
      throws IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    objectMapper.writeValue(response.getWriter(), problem);
  }
}
