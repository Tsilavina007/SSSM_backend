package sssm.dev.sssm_backend.a11_inventory.item_category.dto;

import lombok.Data;

@Data
public class ItemCategoryUpdateRequest {
  private String company_id;
  private String name;
  private String description;
}
