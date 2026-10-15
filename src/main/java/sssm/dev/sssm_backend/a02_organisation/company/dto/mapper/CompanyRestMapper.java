package sssm.dev.sssm_backend.a02_organisation.company.dto.mapper;

import org.springframework.stereotype.Component;

import sssm.dev.sssm_backend.a02_organisation.company.dto.rest.CompanyRest;
import sssm.dev.sssm_backend.a02_organisation.company.entity.Company;

@Component
public class CompanyRestMapper {
  public CompanyRest toRest(Company company) {
    return new CompanyRest(company.getId(), company.getName());
  }
}
