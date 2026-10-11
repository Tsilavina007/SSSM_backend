package sssm.dev.sssm_backend.a11_inventory.item.entity;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import sssm.dev.sssm_backend.a01_common.enums.ItemStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Item {
  private String id;
  private String company_id;
  private String category_id;
  private String item_type_id;
  private String qr_code;
  private String description;
  private ItemStatus status;
  private Timestamp created_at;
  private Timestamp updated_at;
}
