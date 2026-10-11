package sssm.dev.sssm_backend.a11_inventory.warehouse.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Warehouse {
  private String id;
  private String company_id;
  private String branch_id;
  private String name;
  private String address;
  private Boolean active;
}
