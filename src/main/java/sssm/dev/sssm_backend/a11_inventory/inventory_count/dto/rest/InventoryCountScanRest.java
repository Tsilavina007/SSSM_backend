package sssm.dev.sssm_backend.a11_inventory.inventory_count.dto.rest;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryCountScanRest {
  private String id;
  private String inventory_count_id;
  private String item_id;
  private String scanned_by;
  private Timestamp scanned_at;
  private String expected_warehouse_id;
  private String actual_warehouse_id;
  private String discrepancy_type;
  private String notes;
  private String qr_code;
}
