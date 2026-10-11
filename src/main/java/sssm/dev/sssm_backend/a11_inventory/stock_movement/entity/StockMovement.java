package sssm.dev.sssm_backend.a11_inventory.stock_movement.entity;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import sssm.dev.sssm_backend.a01_common.enums.MovementType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockMovement {
  private String id;
  private String warehouse_id;
  private String item_id;
  private String employee_id;
  private MovementType movement_type;
  private String reference;
  private Timestamp movement_date;
  private String notes;
}
