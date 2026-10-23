package sssm.dev.sssm_backend.a01_common.pagination;

import java.util.List;

/**
 * Enveloppe de pagination commune à toutes les listes paginées.
 *
 * @param items éléments de la page demandée
 * @param page page courante (1-based)
 * @param pageSize taille de page appliquée
 * @param total nombre total d'éléments correspondant aux filtres
 * @param totalPages nombre de pages disponibles
 * @param <T> type des éléments
 */
public record PageResponse<T>(
    List<T> items, int page, int pageSize, long total, int totalPages) {

  public PageResponse {
    items = items == null ? List.of() : List.copyOf(items);
  }

  public static <T> PageResponse<T> of(List<T> items, PageQuery query, long total) {
    long safeTotal = Math.max(0, total);
    int totalPages =
        safeTotal == 0
            ? 0
            : (int) Math.min(Integer.MAX_VALUE, (safeTotal + query.pageSize() - 1) / query.pageSize());
    return new PageResponse<>(items, query.page(), query.pageSize(), safeTotal, totalPages);
  }
}
