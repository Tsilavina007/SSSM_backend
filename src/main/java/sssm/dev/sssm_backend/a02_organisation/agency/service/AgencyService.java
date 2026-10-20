package sssm.dev.sssm_backend.a02_organisation.agency.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a02_organisation.agency.dto.AgencyCreateRequest;
import sssm.dev.sssm_backend.a02_organisation.agency.dto.AgencyResponse;
import sssm.dev.sssm_backend.a02_organisation.agency.dto.AgencyUpdateRequest;
import sssm.dev.sssm_backend.a02_organisation.agency.entity.Agency;
import sssm.dev.sssm_backend.a02_organisation.agency.repository.AgencyDAO;

@Service
@RequiredArgsConstructor
public class AgencyService {
  private final AgencyDAO agencyDAO;

  public List<AgencyResponse> getAgencies(
      Optional<String> search, Optional<Integer> page, Optional<Integer> pageSize) {
    int p = page.orElse(1), size = pageSize.orElse(10);
    List<Agency> agencies =
        search
            .map(value -> agencyDAO.searchAgenciesByName(value, p, size))
            .orElseGet(() -> agencyDAO.getAllAgencies(p, size));
    return agencies.stream().map(this::toResponse).toList();
  }

  public AgencyResponse getAgencyById(String id) {
    return agencyDAO
        .getAgencyById(id)
        .map(this::toResponse)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Agence introuvable",
                    "Aucune agence ne correspond à l'identifiant " + id + ".",
                    "NOT_FOUND"));
  }

  public AgencyResponse createAgency(AgencyCreateRequest request) {
    return toResponse(agencyDAO.createAgency(toEntity(request)));
  }

  public AgencyResponse updateAgency(String id, AgencyUpdateRequest request) {
    if (agencyDAO.getAgencyById(id).isEmpty())
      throw new NotFoundException(
          "Agence introuvable",
          "Aucune agence ne correspond à l'identifiant " + id + ".",
          "NOT_FOUND");
    Agency agency = toEntity(request);
    agency.setId(id);
    return toResponse(agencyDAO.updateAgency(agency));
  }

  public void deleteAgency(String id) {
    if (agencyDAO.getAgencyById(id).isPresent()) agencyDAO.deleteAgency(id);
  }

  private Agency toEntity(AgencyCreateRequest request) {
    Agency agency = new Agency();
    agency.setCompany_id(request.getCompany_id());
    agency.setName(request.getName());
    agency.setCode(request.getCode());
    agency.setAddress(request.getAddress());
    agency.setCity(request.getCity());
    agency.setPhone(request.getPhone());
    agency.setEmail(request.getEmail());
    agency.setTax_number(request.getTax_number());
    agency.setRegistration_number(request.getRegistration_number());
    agency.setActive(request.getActive());
    return agency;
  }

  private Agency toEntity(AgencyUpdateRequest request) {
    Agency agency = new Agency();
    agency.setCompany_id(request.getCompany_id());
    agency.setName(request.getName());
    agency.setCode(request.getCode());
    agency.setAddress(request.getAddress());
    agency.setCity(request.getCity());
    agency.setPhone(request.getPhone());
    agency.setEmail(request.getEmail());
    agency.setTax_number(request.getTax_number());
    agency.setRegistration_number(request.getRegistration_number());
    agency.setActive(request.getActive());
    return agency;
  }

  private AgencyResponse toResponse(Agency agency) {
    return new AgencyResponse(
        agency.getId(),
        agency.getCompany_id(),
        agency.getName(),
        agency.getCode(),
        agency.getAddress(),
        agency.getCity(),
        agency.getPhone(),
        agency.getEmail(),
        agency.getTax_number(),
        agency.getRegistration_number(),
        agency.getActive(),
        agency.getCreated_at(),
        agency.getUpdated_at());
  }
}
