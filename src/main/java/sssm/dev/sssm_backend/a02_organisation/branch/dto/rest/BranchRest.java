package sssm.dev.sssm_backend.a02_organisation.branch.dto.rest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import sssm.dev.sssm_backend.a02_organisation.agency.dto.rest.AgencyRest;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BranchRest {
  private String id;
  private String company_id;
  private AgencyRest agence;
  private String name;
}
