package sssm.dev.sssm_backend.a11_inventory.item.entity;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import sssm.dev.sssm_backend.a01_common.enums.ItemStatus;
import sssm.dev.sssm_backend.a02_organisation.company.entity.Company;
import sssm.dev.sssm_backend.a11_inventory.item_category.entity.ItemCategory;
import sssm.dev.sssm_backend.a11_inventory.item_type.entity.ItemType;
import sssm.dev.sssm_backend.a11_inventory.warehouse.entity.Warehouse;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Item {
  private String id;
  private Company company;
  private ItemCategory category;
  private ItemType item_type;
  private String qr_code;
  private String description;
  private ItemStatus status;
  private Warehouse current_warehouse;
  private Timestamp created_at;
  private Timestamp updated_at;
  private Timestamp archived_at;
  private String archived_by;
  private Timestamp last_inventory_at;
}
