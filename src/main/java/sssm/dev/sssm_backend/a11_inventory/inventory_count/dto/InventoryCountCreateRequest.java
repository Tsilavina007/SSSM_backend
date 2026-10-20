package sssm.dev.sssm_backend.a11_inventory.inventory_count.dto;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;

@Data
public class InventoryCountCreateRequest {
  private String company_id;
  private String warehouse_id;
  private String reference;
  private String status;
  private String started_by;
  private Timestamp started_at;
  private String completed_by;
  private Timestamp completed_at;
  private String notes;

  public Map<String, Object> toMap() {
    Map<String, Object> values = new LinkedHashMap<>();
    put(values, "company_id", company_id);
    put(values, "warehouse_id", warehouse_id);
    put(values, "reference", reference);
    put(values, "status", status);
    put(values, "started_by", started_by);
    put(values, "started_at", started_at);
    put(values, "completed_by", completed_by);
    put(values, "completed_at", completed_at);
    put(values, "notes", notes);
    return values;
  }

  private static void put(Map<String, Object> values, String key, Object value) {
    if (value != null) {
      values.put(key, value);
    }
  }
}
