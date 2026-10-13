package sssm.dev.sssm_backend.a11_inventory.warehouse.entity;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import sssm.dev.sssm_backend.a02_organisation.branch.entity.Branch;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Warehouse {
  private String id;
  private String company_id;
  private Branch branch;
  private String name;
  private String address;
  private Boolean active;
  private Timestamp created_at;
  private Timestamp updated_at;
  private Timestamp archived_at;
}
