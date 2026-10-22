package sssm.dev.sssm_backend.a11_inventory.operation.dto;

import java.sql.Timestamp;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperationResponse {
  private String id;
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
  private Timestamp created_at;
  private Timestamp completed_at;
  private List<OperationLineResponse> lines;
}
