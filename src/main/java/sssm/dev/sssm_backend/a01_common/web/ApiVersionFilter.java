package sssm.dev.sssm_backend.a01_common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Fait de {@code /api/v1} le préfixe canonique de l'API.
 *
 * <p>Les routes des contrôleurs sont déclarées sous {@code /api/v1}. Les appels historiques sous
 * {@code /api} restent acceptés pendant la période de dépréciation : ils sont réécrits vers
 * {@code /api/v1} avant la sécurité et la résolution des routes, et la réponse porte les en-têtes
 * {@code Deprecation} et {@code Link} vers la ressource versionnée.
 *
 * <p>Le filtre s'exécute avant Spring Security pour que les règles d'autorisation voient toujours
 * le chemin versionné.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiVersionFilter extends OncePerRequestFilter {

  static final String VERSIONED_PREFIX = "/api/v1";
  static final String LEGACY_PREFIX = "/api";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String uri = request.getRequestURI();
    if (isVersioned(uri) || !isLegacy(uri)) {
      chain.doFilter(request, response);
      return;
    }

    String versionedUri = VERSIONED_PREFIX + uri.substring(LEGACY_PREFIX.length());
    response.setHeader("Deprecation", "true");
    response.setHeader("Link", "<" + versionedUri + ">; rel=\"successor-version\"");
    chain.doFilter(new VersionedRequest(request, versionedUri), response);
  }

  private static boolean isVersioned(String uri) {
    return uri.equals(VERSIONED_PREFIX) || uri.startsWith(VERSIONED_PREFIX + "/");
  }

  private static boolean isLegacy(String uri) {
    return uri.equals(LEGACY_PREFIX) || uri.startsWith(LEGACY_PREFIX + "/");
  }

  private static final class VersionedRequest extends HttpServletRequestWrapper {
    private final String uri;

    private VersionedRequest(HttpServletRequest request, String uri) {
      super(request);
      this.uri = uri;
    }

    @Override
    public String getRequestURI() {
      return uri;
    }

    @Override
    public StringBuffer getRequestURL() {
      StringBuffer url = new StringBuffer();
      String scheme = getScheme();
      int port = getServerPort();
      url.append(scheme).append("://").append(getServerName());
      if (("http".equals(scheme) && port != 80) || ("https".equals(scheme) && port != 443)) {
        url.append(':').append(port);
      }
      url.append(uri);
      return url;
    }

    @Override
    public String getServletPath() {
      return uri;
    }
  }
}
