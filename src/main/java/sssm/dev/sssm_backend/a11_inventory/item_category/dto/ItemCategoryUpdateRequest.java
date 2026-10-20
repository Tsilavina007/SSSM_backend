package sssm.dev.sssm_backend.a11_inventory.item_category.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ItemCategoryUpdateRequest {
  @Size(max = 36, message = "36 caractères maximum")
  private String company_id;

  @Size(max = 150, message = "150 caractères maximum")
  private String name;

  private String description;
}
