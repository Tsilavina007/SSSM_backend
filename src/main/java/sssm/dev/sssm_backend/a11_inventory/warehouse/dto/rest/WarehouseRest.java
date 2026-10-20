package sssm.dev.sssm_backend.a11_inventory.warehouse.dto.rest;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import sssm.dev.sssm_backend.a02_organisation.branch.dto.rest.BranchRest;
import sssm.dev.sssm_backend.a02_organisation.company.dto.rest.CompanyRest;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseRest {
  private String id;
  private CompanyRest company_id;
  private BranchRest branch;
  private String name;
  private String address;
  private Boolean active;
  private Timestamp created_at;
  private Timestamp updated_at;
  private Timestamp archived_at;
}
