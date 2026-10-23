package sssm.dev.sssm_backend.a01_common.web;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Map;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Ajoute l'en-tête {@code Location} aux créations qui renvoient 201 et une ressource identifiée.
 *
 * <p>L'URI est construite à partir de l'URL de la collection et de l'identifiant de la ressource
 * créée. Les corps sans identifiant exploitable (réponses de type {@code Map}, créations en lot)
 * sont ignorés : il n'existe pas d'URI canonique fiable pour eux.
 */
@ControllerAdvice
public class CreatedLocationAdvice implements ResponseBodyAdvice<Object> {

  @Override
  public boolean supports(
      MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
    return true;
  }

  @Override
  public Object beforeBodyWrite(
      Object body,
      MethodParameter returnType,
      MediaType selectedContentType,
      Class<? extends HttpMessageConverter<?>> selectedConverterType,
      ServerHttpRequest request,
      ServerHttpResponse response) {
    if (body == null
        || body instanceof Map
        || !(response instanceof org.springframework.http.server.ServletServerHttpResponse servlet)
        || servlet.getServletResponse().getStatus() != HttpStatus.CREATED.value()
        || response.getHeaders().getLocation() != null) {
      return body;
    }

    String id = identifier(body);
    if (id == null || id.isBlank()) {
      return body;
    }

    String path = request.getURI().getRawPath();
    String location = path.endsWith("/") ? path + id : path + "/" + id;
    response.getHeaders().setLocation(URI.create(location));
    return body;
  }

  private static String identifier(Object body) {
    Object value = invoke(body, "id");
    if (value == null) {
      value = invoke(body, "getId");
    }
    return value == null ? null : String.valueOf(value);
  }

  private static Object invoke(Object body, String methodName) {
    try {
      Method method = body.getClass().getMethod(methodName);
      if (method.getParameterCount() != 0) {
        return null;
      }
      return method.invoke(body);
    } catch (ReflectiveOperationException | RuntimeException ignored) {
      return null;
    }
  }
}
