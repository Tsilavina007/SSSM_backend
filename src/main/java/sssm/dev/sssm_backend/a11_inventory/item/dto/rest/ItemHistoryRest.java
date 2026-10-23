package sssm.dev.sssm_backend.a11_inventory.item.dto.rest;

import sssm.dev.sssm_backend.a01_common.pagination.PageResponse;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.dto.rest.ItemAssignmentRest;
import sssm.dev.sssm_backend.a11_inventory.stock_movement.dto.rest.StockMovementRest;

/**
 * Historique paginé d'un article : mouvements et affectations.
 *
 * @param item_id article concerné
 * @param movements mouvements filtrés et paginés
 * @param assignments affectations filtrées et paginées
 */
public record ItemHistoryRest(
    String item_id,
    PageResponse<StockMovementRest> movements,
    PageResponse<ItemAssignmentRest> assignments) {}
