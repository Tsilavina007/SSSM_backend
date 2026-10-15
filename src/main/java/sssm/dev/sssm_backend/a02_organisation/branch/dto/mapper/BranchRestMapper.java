package sssm.dev.sssm_backend.a02_organisation.branch.dto.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import sssm.dev.sssm_backend.a02_organisation.agency.dto.mapper.AgencyRestMapper;
import sssm.dev.sssm_backend.a02_organisation.agency.entity.Agency;
import sssm.dev.sssm_backend.a02_organisation.agency.repository.AgencyDAOImpl;
import sssm.dev.sssm_backend.a02_organisation.branch.dto.BranchResponse;
import sssm.dev.sssm_backend.a02_organisation.branch.dto.rest.BranchRest;

@Component
public class BranchRestMapper {
  @Autowired private AgencyDAOImpl agencyDAOImpl;
  @Autowired private AgencyRestMapper agencyRestMapper;

  public BranchRest toRest(BranchResponse branch) {
    Agency agency = agencyDAOImpl.getAgencyById(branch.getAgence_id()).get();
    return new BranchRest(
        branch.getId(), branch.getCompany_id(), agencyRestMapper.toRest(agency), branch.getName());
  }
}
