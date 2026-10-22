package sssm.dev.sssm_backend.a11_inventory.operation.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperationLineResponse {
  private String id;
  private String operation_id;
  private String item_id;
  private String movement_id;
  private BigDecimal quantity;
  private String condition_before;
  private String condition_after;
  private Timestamp scanned_at;
  private String notes;
}
