package sssm.dev.sssm_backend.a11_inventory.qr_scan.dto;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QrScanItemResponse {
  private String id;
  private String company_id;
  private String category_id;
  private String item_type_id;
  private String qr_code;
  private String description;
  private String status;
  private String current_warehouse_id;
  private Timestamp created_at;
  private Timestamp updated_at;
  private Timestamp archived_at;
  private String archived_by;
  private Timestamp last_inventory_at;
}
