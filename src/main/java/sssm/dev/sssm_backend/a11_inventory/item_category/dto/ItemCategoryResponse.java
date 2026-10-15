package sssm.dev.sssm_backend.a11_inventory.item_category.dto;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemCategoryResponse {
  private String id;
  private String company_id;
  private String name;
  private String description;
  private Timestamp created_at;
  private Timestamp updated_at;
  private Timestamp archived_at;
}
