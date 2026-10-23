package sssm.dev.sssm_backend.a11_inventory.inventory_count.dto;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;

/**
 * Corps de clôture d'un comptage d'inventaire.
 *
 * <p>Le statut et l'horodatage de clôture sont pilotés par le serveur ; l'auteur est le porteur du
 * jeton, et {@code completed_by} ne sert que de repli hors contexte authentifié.
 */
@Data
public class InventoryCountCloseRequest {
  private String completed_by;
  private String notes;

  public Map<String, Object> toMap() {
    Map<String, Object> values = new LinkedHashMap<>();
    put(values, "completed_by", completed_by);
    put(values, "notes", notes);
    return values;
  }

  private static void put(Map<String, Object> values, String key, Object value) {
    if (value != null) {
      values.put(key, value);
    }
  }
}
