package sssm.dev.sssm_backend.a11_inventory.inventory_count.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;

@Data
public class InventoryCountScanRequest {
  private String inventory_count_id;
  private String item_id;
  private String scanned_by;
  private Timestamp scanned_at;
  private String expected_warehouse_id;
  private String actual_warehouse_id;
  private String discrepancy_type;
  private String notes;

  public Map<String, Object> toMap() {
    Map<String, Object> values = new LinkedHashMap<>();
    put(values, "inventory_count_id", inventory_count_id);
    put(values, "item_id", item_id);
    put(values, "scanned_by", scanned_by);
    put(values, "scanned_at", scanned_at);
    put(values, "expected_warehouse_id", expected_warehouse_id);
    put(values, "actual_warehouse_id", actual_warehouse_id);
    put(values, "discrepancy_type", discrepancy_type);
    put(values, "notes", notes);
    return values;
  }

  @JsonAnySetter
  public void rejectUnknownField(String name, Object value) {
    throw new IllegalArgumentException(
        "Le champ '" + name + "' n'est pas autorisé pour un scan de comptage.");
  }

  private static void put(Map<String, Object> values, String key, Object value) {
    if (value != null) {
      values.put(key, value);
    }
  }
}
