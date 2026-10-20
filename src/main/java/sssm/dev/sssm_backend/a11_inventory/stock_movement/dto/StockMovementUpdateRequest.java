package sssm.dev.sssm_backend.a11_inventory.stock_movement.dto;

import jakarta.validation.constraints.Size;
import java.sql.Timestamp;
import lombok.Data;

import sssm.dev.sssm_backend.a01_common.enums.MovementType;

@Data
public class StockMovementUpdateRequest {
  @Size(max = 36, message = "36 caractères maximum")
  private String warehouse_id;

  @Size(max = 36, message = "36 caractères maximum")
  private String item_id;

  @Size(max = 36, message = "36 caractères maximum")
  private String employee_id;

  private MovementType movement_type;

  @Size(max = 150, message = "150 caractères maximum")
  private String reference;

  private Timestamp movement_date;
  private String notes;
}
