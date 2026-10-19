package sssm.dev.sssm_backend.a11_inventory.dashboard.dto.rest;

import java.math.BigDecimal;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardLowStockRest {
  private String id;
  private String company_id;
  private String category_id;
  private String code;
  private String name;
  private String description;
  private String unit;
  private BigDecimal quantity_total;
  private BigDecimal minimum_stock;
  private String status;
  private Timestamp created_at;
  private Timestamp updated_at;
  private Timestamp archived_at;
  private Long stock_quantity;
}
