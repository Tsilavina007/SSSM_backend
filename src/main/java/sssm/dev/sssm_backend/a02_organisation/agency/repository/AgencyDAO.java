package sssm.dev.sssm_backend.a02_organisation.agency.repository;

import java.util.List;
import java.util.Optional;

import sssm.dev.sssm_backend.a02_organisation.agency.entity.Agency;

public interface AgencyDAO {
  List<Agency> getAllAgencies(int page, int pageSize);

  List<Agency> searchAgenciesByName(String searchName, int page, int pageSize);

  Optional<Agency> getAgencyById(String agencyId);

  Agency createAgency(Agency agency);

  Agency updateAgency(Agency agency);

  void deleteAgency(String agencyId);
}
