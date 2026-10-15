package sssm.dev.sssm_backend.a11_inventory.item.dto.mapper;

import org.springframework.stereotype.Component;

import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemResponse;
import sssm.dev.sssm_backend.a11_inventory.item.dto.rest.ItemQrCodeRest;

@Component
public class ItemQrCodeRestMapper {
  public ItemQrCodeRest toRest(ItemResponse rs) {
    return (new ItemQrCodeRest(
        rs.getId(),
        rs.getCompany_id(),
        rs.getCategory_id(),
        rs.getItem_type_id(),
        rs.getCurrent_warehouse_id(),
        rs.getQr_code()));
  }
}
