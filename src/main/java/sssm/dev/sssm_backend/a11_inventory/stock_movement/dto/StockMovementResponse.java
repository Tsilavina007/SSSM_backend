package sssm.dev.sssm_backend.a11_inventory.stock_movement.dto;

import java.sql.Timestamp;
import lombok.Data;

import sssm.dev.sssm_backend.a01_common.enums.MovementType;

@Data
public class StockMovementResponse {
  private String id;
  private String warehouse_id;
  private String item_id;
  private String employee_id;
  private MovementType movement_type;
  private String reference;
  private Timestamp movement_date;
  private String notes;
}
