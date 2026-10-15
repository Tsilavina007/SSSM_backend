package sssm.dev.sssm_backend.a11_inventory.item.dto.rest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemQrCodeRest {
  private String id;
  private String company_id;
  private String category_id;
  private String item_type_id;
  private String current_warehouse_id;
  private String qr_code;
}
