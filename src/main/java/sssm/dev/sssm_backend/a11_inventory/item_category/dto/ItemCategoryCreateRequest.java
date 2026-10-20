package sssm.dev.sssm_backend.a11_inventory.item_category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ItemCategoryCreateRequest {
  @NotBlank(message = "L'identifiant de l'entreprise est obligatoire")
  @Size(max = 36, message = "36 caractères maximum")
  private String company_id;

  @NotBlank(message = "Le nom de la catégorie est obligatoire")
  @Size(max = 150, message = "150 caractères maximum")
  private String name;

  private String description;
}
