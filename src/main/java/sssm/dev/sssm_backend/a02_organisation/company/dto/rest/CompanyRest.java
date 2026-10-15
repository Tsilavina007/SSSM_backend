package sssm.dev.sssm_backend.a02_organisation.company.dto.rest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Company Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompanyRest {
  private String id;
  private String name;
}
