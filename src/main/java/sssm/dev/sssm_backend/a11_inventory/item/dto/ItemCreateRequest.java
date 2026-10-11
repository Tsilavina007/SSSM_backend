package sssm.dev.sssm_backend.a11_inventory.item.dto;

import lombok.Data;
import sssm.dev.sssm_backend.a01_common.enums.ItemStatus;

@Data
public class ItemCreateRequest {
  private String company_id;
  private String category_id;
  private String item_type_id;
  private String qr_code;
  private String description;
  private ItemStatus status;
}
