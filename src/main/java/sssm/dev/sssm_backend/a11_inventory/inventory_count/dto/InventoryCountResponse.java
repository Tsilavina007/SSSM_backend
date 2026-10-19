package sssm.dev.sssm_backend.a11_inventory.inventory_count.dto;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryCountResponse {
  private String id;
  private String company_id;
  private String warehouse_id;
  private String reference;
  private String status;
  private String started_by;
  private String completed_by;
  private Timestamp started_at;
  private Timestamp completed_at;
  private String notes;
}
