package sssm.dev.sssm_backend.a02_organisation.agency.dto.mapper;

import org.springframework.stereotype.Component;

import sssm.dev.sssm_backend.a02_organisation.agency.dto.rest.AgencyRest;
import sssm.dev.sssm_backend.a02_organisation.agency.entity.Agency;

@Component
public class AgencyRestMapper {
  public AgencyRest toRest(Agency agency) {
    return new AgencyRest(agency.getId(), agency.getCompany_id(), agency.getName());
  }
}
