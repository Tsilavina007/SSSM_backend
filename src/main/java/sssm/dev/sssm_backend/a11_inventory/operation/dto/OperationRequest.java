package sssm.dev.sssm_backend.a11_inventory.operation.dto;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;

@Data
public class OperationRequest {
  private String company_id;
  private String operation_type;
  private String status;
  private String reference;
  private String idempotency_key;
  private String source_warehouse_id;
  private String destination_warehouse_id;
  private String employee_id;
  private String post_id;
  private String performed_by;
  private String notes;
  private Timestamp completed_at;

  public Map<String, Object> toMap() {
    Map<String, Object> values = new LinkedHashMap<>();
    put(values, "company_id", company_id);
    put(values, "operation_type", operation_type);
    put(values, "status", status);
    put(values, "reference", reference);
    put(values, "idempotency_key", idempotency_key);
    put(values, "source_warehouse_id", source_warehouse_id);
    put(values, "destination_warehouse_id", destination_warehouse_id);
    put(values, "employee_id", employee_id);
    put(values, "post_id", post_id);
    put(values, "performed_by", performed_by);
    put(values, "notes", notes);
    put(values, "completed_at", completed_at);
    return values;
  }

  private static void put(Map<String, Object> values, String key, Object value) {
    if (value != null) {
      values.put(key, value);
    }
  }
}
