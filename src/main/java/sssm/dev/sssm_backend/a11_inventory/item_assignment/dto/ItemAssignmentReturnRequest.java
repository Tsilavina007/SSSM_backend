package sssm.dev.sssm_backend.a11_inventory.item_assignment.dto;

import lombok.Data;

/**
 * Corps facultatif du retour d'une affectation.
 *
 * <p>La date et l'acteur du retour sont pilotés par le serveur ; {@code condition_on_return} est le
 * seul champ lu, conformément au contrat {@code POST /api/v1/item-assignments/{id}/return}.
 */
@Data
public class ItemAssignmentReturnRequest {
  private String condition_on_return;
}
