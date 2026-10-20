package sssm.dev.sssm_backend.a11_inventory.item.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import sssm.dev.sssm_backend.a01_common.enums.ItemStatus;

@Data
public class ItemCreateRequest {
  @NotBlank(message = "L'identifiant de l'entreprise est obligatoire")
  @Size(max = 36, message = "36 caractères maximum")
  private String company_id;

  @Size(max = 36, message = "36 caractères maximum")
  private String category_id;

  @NotBlank(message = "L'identifiant du type d'article est obligatoire")
  @Size(max = 36, message = "36 caractères maximum")
  private String item_type_id;

  @Size(max = 100, message = "100 caractères maximum")
  private String qr_code;

  private String description;
  private ItemStatus status;

  @Size(max = 36, message = "36 caractères maximum")
  private String current_warehouse_id;
}
