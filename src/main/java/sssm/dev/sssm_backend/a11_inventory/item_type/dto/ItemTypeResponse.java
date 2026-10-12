package sssm.dev.sssm_backend.a11_inventory.item_type.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;
import lombok.Data;

import sssm.dev.sssm_backend.a01_common.enums.ItemStatus;

@Data
public class ItemTypeResponse {
  private String id;
  private String company_id;
  private String category_id;
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
