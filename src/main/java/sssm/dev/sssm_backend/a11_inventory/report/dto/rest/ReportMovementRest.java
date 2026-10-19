package sssm.dev.sssm_backend.a11_inventory.report.dto.rest;

import java.math.BigDecimal;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportMovementRest {
  private String id;
  private String warehouse_id;
  private String item_id;
  private String employee_id;
  private String movement_type;
  private String reference;
  private Timestamp movement_date;
  private String notes;
  private String source_warehouse_id;
  private String destination_warehouse_id;
  private String operation_id;
  private BigDecimal quantity;
  private String performed_by;
  private Timestamp created_at;
}
