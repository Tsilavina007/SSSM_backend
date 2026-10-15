package sssm.dev.sssm_backend.a02_organisation.agency.dto.rest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgencyRest {
  private String id;
  private String company_id;
  private String name;
}
