package sssm.dev.sssm_backend.a11_inventory.item_category.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemCategory {
  private String id;
  private String company_id;
  private String name;
  private String description;
}
