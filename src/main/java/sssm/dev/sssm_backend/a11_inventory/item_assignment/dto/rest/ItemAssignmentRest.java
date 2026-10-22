package sssm.dev.sssm_backend.a11_inventory.item_assignment.dto.rest;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemAssignmentRest {
  private String id;
  private String company_id;
  private String item_id;
  private String employee_id;
  private String post_id;
  private Timestamp assigned_at;
  private Timestamp returned_at;
  private String condition_on_assignment;
  private String condition_on_return;
  private String notes;
  private Timestamp created_at;
  private Timestamp updated_at;
  private String assigned_by;
  private String returned_by;
  private Timestamp expected_return_at;
  private String source_warehouse_id;
  private Timestamp voided_at;
  private String voided_by;
  private String void_reason;
}
