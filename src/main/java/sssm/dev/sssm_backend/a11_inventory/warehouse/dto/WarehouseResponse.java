package sssm.dev.sssm_backend.a11_inventory.warehouse.dto;

import lombok.Data;

@Data
public class WarehouseResponse {
  private String id;
  private String company_id;
  private String branch_id;
  private String name;
  private String address;
  private Boolean active;
}
