package sssm.dev.sssm_backend.a11_inventory.item_type.entity;

import java.math.BigDecimal;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import sssm.dev.sssm_backend.a01_common.enums.ItemStatus;
import sssm.dev.sssm_backend.a02_organisation.company.entity.Company;
import sssm.dev.sssm_backend.a11_inventory.item_category.entity.ItemCategory;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemType {
  private String id;
  private Company company;
  private ItemCategory category;
  private String code;
  private String name;
  private String description;
  private String unit;
  private BigDecimal quantity_total;
  private BigDecimal minimum_stock;
  private ItemStatus status;
  private Timestamp created_at;
  private Timestamp updated_at;
}
