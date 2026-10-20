package sssm.dev.sssm_backend.a11_inventory.item_type.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

import sssm.dev.sssm_backend.a01_common.enums.ItemStatus;

@Data
public class ItemTypeCreateRequest {
  @NotBlank(message = "L'identifiant de l'entreprise est obligatoire")
  @Size(max = 36, message = "36 caractères maximum")
  private String company_id;

  @Size(max = 36, message = "36 caractères maximum")
  private String category_id;

  @NotBlank(message = "Le code du type d'article est obligatoire")
  @Size(max = 100, message = "100 caractères maximum")
  private String code;

  @NotBlank(message = "Le nom du type d'article est obligatoire")
  @Size(max = 180, message = "180 caractères maximum")
  private String name;

  private String description;

  @Size(max = 50, message = "50 caractères maximum")
  private String unit;

  @Min(value = 0, message = "La quantité totale doit être positive ou nulle")
  private BigDecimal quantity_total;

  @Min(value = 0, message = "Le stock minimum doit être positif ou nul")
  private BigDecimal minimum_stock;

  private ItemStatus status;
}
