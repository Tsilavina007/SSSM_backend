package sssm.dev.sssm_backend.a11_inventory.stock_balance.dto.rest;

import java.math.BigDecimal;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockBalanceRest {
  private String id;
  private String company_id;
  private String warehouse_id;
  private String warehouse_name;
  private String item_type_id;
  private String item_type_code;
  private String item_type_name;
  private BigDecimal quantity;
  private BigDecimal minimum_stock;
  private boolean below_minimum;
  private Long version;
  private Timestamp updated_at;
}
