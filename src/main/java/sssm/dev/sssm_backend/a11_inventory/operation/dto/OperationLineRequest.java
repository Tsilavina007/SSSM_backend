package sssm.dev.sssm_backend.a11_inventory.operation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class OperationLineRequest {
  @NotBlank(message = "L'identifiant de l'article est obligatoire")
  @Size(max = 36, message = "36 caractères maximum")
  private String item_id;

  @DecimalMin(value = "0.01", message = "La quantité doit être strictement positive")
  @Digits(integer = 10, fraction = 2, message = "La quantité doit avoir au maximum 2 décimales")
  private BigDecimal quantity;

  @Size(max = 36, message = "36 caractères maximum")
  private String source_warehouse_id;

  @Size(max = 36, message = "36 caractères maximum")
  private String destination_warehouse_id;

  private String notes;
}
