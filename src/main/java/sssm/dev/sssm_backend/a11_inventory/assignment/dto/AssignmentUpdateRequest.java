package sssm.dev.sssm_backend.a11_inventory.assignment.dto;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;

@Data
public class AssignmentUpdateRequest {
  private String item_id;
  private String employee_id;
  private String post_id;
  private Timestamp assigned_at;
  private Timestamp returned_at;
  private String condition_on_assignment;
  private String condition_on_return;
  private String notes;
  private String assigned_by;
  private String returned_by;
  private Timestamp expected_return_at;

  public Map<String, Object> toMap() {
    Map<String, Object> values = new LinkedHashMap<>();
    put(values, "item_id", item_id);
    put(values, "employee_id", employee_id);
    put(values, "post_id", post_id);
    put(values, "assigned_at", assigned_at);
    put(values, "returned_at", returned_at);
    put(values, "condition_on_assignment", condition_on_assignment);
    put(values, "condition_on_return", condition_on_return);
    put(values, "notes", notes);
    put(values, "assigned_by", assigned_by);
    put(values, "returned_by", returned_by);
    put(values, "expected_return_at", expected_return_at);
    return values;
  }

  private static void put(Map<String, Object> values, String key, Object value) {
    if (value != null) {
      values.put(key, value);
    }
  }
}
