package sssm.dev.sssm_backend.a01_common.pagination;

/**
 * Paramètres de pagination normalisés d'une liste.
 *
 * <p>La page est 1-based et ne peut pas être inférieure à 1 ; la taille de page est bornée pour
 * éviter qu'un client ne charge toute une table. Les valeurs absentes ou invalides retombent sur
 * les valeurs par défaut du point d'appel.
 */
public record PageQuery(int page, int pageSize) {
  public static final int DEFAULT_PAGE_SIZE = 20;
  public static final int MAX_PAGE_SIZE = 500;

  public PageQuery {
    if (page < 1) {
      page = 1;
    }
    if (pageSize < 1) {
      pageSize = DEFAULT_PAGE_SIZE;
    }
    if (pageSize > MAX_PAGE_SIZE) {
      pageSize = MAX_PAGE_SIZE;
    }
  }

  public static PageQuery of(Integer page, Integer pageSize) {
    return of(page, pageSize, DEFAULT_PAGE_SIZE);
  }

  public static PageQuery of(Integer page, Integer pageSize, int defaultPageSize) {
    return new PageQuery(page == null ? 1 : page, pageSize == null ? defaultPageSize : pageSize);
  }

  public int offset() {
    return (page - 1) * pageSize;
  }
}
