package sssm.dev.sssm_backend.a11_inventory.item_assignment.dto;

import java.sql.Timestamp;
import lombok.Data;

@Data
public class ItemAssignmentUpdateRequest {
  private String item_id;
  private String employee_id;
  private String post_id;
  private Timestamp assigned_at;
  private Timestamp returned_at;
  private String condition_on_assignment;
  private String condition_on_return;
  private String notes;
}
