package sssm.dev.sssm_backend.a01_common.errors;

/**
 * Erreur applicative unique renvoyée par toutes les routes.
 *
 * @param message message lisible en français
 * @param detail précision contextuelle (identifiant, champ, contrainte SQL, cause)
 * @param code code applicatif stable ou SQLState PostgreSQL
 */
public record ItemError(String message, String detail, String code) {
  public ItemError {
    message = defaultIfBlank(message, "Erreur inattendue");
    detail = defaultIfBlank(detail, "Aucun détail supplémentaire n'est disponible.");
    code = defaultIfBlank(code, "INTERNAL_ERROR");
  }

  private static String defaultIfBlank(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }
}
