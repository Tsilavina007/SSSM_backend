package sssm.dev.sssm_backend.a11_inventory.item.entity;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import sssm.dev.sssm_backend.a01_common.enums.ItemStatus;
import sssm.dev.sssm_backend.a02_organisation.company.dto.rest.CompanyRest;
import sssm.dev.sssm_backend.a11_inventory.item_category.dto.rest.ItemCategoryRest;
import sssm.dev.sssm_backend.a11_inventory.item_type.dto.rest.ItemTypeRest;
import sssm.dev.sssm_backend.a11_inventory.warehouse.dto.rest.WarehouseRest;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Item {
  private String id;
  private CompanyRest company;
  private ItemCategoryRest category;
  private ItemTypeRest item_type;
  private WarehouseRest current_warehouse;
  private String qr_code;
  private String description;
  private ItemStatus status;
  private Timestamp created_at;
  private Timestamp updated_at;
  private Timestamp archived_at;
  private String archived_by;
  private Timestamp last_inventory_at;
}
