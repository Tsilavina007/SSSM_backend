package sssm.dev.sssm_backend.a11_inventory.inventory_count.dto;

import lombok.Data;

/**
 * Corps de création d'un comptage d'inventaire.
 *
 * <p>L'entreprise, le statut, l'auteur et l'horodatage de départ sont pilotés par le serveur : le
 * client ne choisit que l'entrepôt compté, la référence et une note libre.
 */
@Data
public class InventoryCountCreateRequest {
  private String warehouse_id;
  private String reference;
  private String notes;
}
