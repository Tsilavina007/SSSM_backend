package sssm.dev.sssm_backend.a11_inventory.stock_movement.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.sql.Timestamp;
import lombok.Data;

import sssm.dev.sssm_backend.a01_common.enums.MovementType;

@Data
public class StockMovementCreateRequest {
  @NotBlank(message = "L'identifiant de l'entrepôt est obligatoire")
  @Size(max = 36, message = "36 caractères maximum")
  private String warehouse_id;

  @NotBlank(message = "L'identifiant de l'article est obligatoire")
  @Size(max = 36, message = "36 caractères maximum")
  private String item_id;

  @Size(max = 36, message = "36 caractères maximum")
  private String employee_id;

  // Requis pour OUT : l'affectation créée doit porter un poste.
  @Size(max = 36, message = "36 caractères maximum")
  private String post_id;

  // Facultatif : le DAO applique le type IN lorsque le champ est absent.
  private MovementType movement_type;

  @DecimalMin(value = "0.01", message = "La quantité doit être strictement positive")
  @Digits(integer = 10, fraction = 2, message = "La quantité doit avoir au maximum 2 décimales")
  private BigDecimal quantity;

  @Size(max = 36, message = "36 caractères maximum")
  private String source_warehouse_id;

  @Size(max = 36, message = "36 caractères maximum")
  private String destination_warehouse_id;

  @Size(max = 36, message = "36 caractères maximum")
  private String operation_id;

  @Size(max = 36, message = "36 caractères maximum")
  private String performed_by;

  @Size(max = 150, message = "150 caractères maximum")
  private String reference;

  private Timestamp movement_date;
  private String notes;
}
