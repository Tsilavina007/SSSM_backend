package sssm.dev.sssm_backend.a02_organisation.agency.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a02_organisation.agency.entity.Agency;
import sssm.dev.sssm_backend.a02_organisation.agency.repository.AgencyDAO;

@Service
@RequiredArgsConstructor
public class AgencyService {
  private final AgencyDAO agencyDAO;

  public List<Agency> getAgencies(
      Optional<String> search, Optional<Integer> page, Optional<Integer> pageSize) {
    int p = page.orElse(1), size = pageSize.orElse(10);
    return search.isPresent()
        ? agencyDAO.searchAgenciesByName(search.get(), p, size)
        : agencyDAO.getAllAgencies(p, size);
  }

  public Optional<Agency> getAgencyById(String id) {
    return agencyDAO.getAgencyById(id);
  }

  public Agency createAgency(Agency agency) {
    return agencyDAO.createAgency(agency);
  }

  public Agency updateAgency(String id, Agency agency) {
    if (agencyDAO.getAgencyById(id).isEmpty())
      throw new IllegalArgumentException("Agency not found with ID: " + id);
    agency.setId(id);
    return agencyDAO.updateAgency(agency);
  }

  public void deleteAgency(String id) {
    if (agencyDAO.getAgencyById(id).isPresent()) agencyDAO.deleteAgency(id);
  }
}
