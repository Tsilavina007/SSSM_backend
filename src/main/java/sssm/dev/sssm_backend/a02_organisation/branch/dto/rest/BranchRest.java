package sssm.dev.sssm_backend.a02_organisation.branch.dto.rest;

import java.sql.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import sssm.dev.sssm_backend.a01_common.enums.SiteStatus;
import sssm.dev.sssm_backend.a02_organisation.agency.dto.rest.AgencyRest;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BranchRest {
  private String id;
  private String company_id;
  private AgencyRest agence;
  private String name;
  private String code;
  private String address;
  private String city;
  private String phone;
  private String email;
  private SiteStatus status;
  private Date created_at;
  private Date updated_at;
}
