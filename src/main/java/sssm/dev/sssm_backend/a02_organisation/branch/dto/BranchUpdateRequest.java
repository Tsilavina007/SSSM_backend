package sssm.dev.sssm_backend.a02_organisation.branch.dto;

import lombok.Data;

import sssm.dev.sssm_backend.a01_common.enums.SiteStatus;

@Data
public class BranchUpdateRequest {
  private String company_id;
  private String agence_id;
  private String name;
  private String code;
  private String address;
  private String city;
  private String phone;
  private String email;
  private SiteStatus status;
}
