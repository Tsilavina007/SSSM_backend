package sssm.dev.sssm_backend.a02_organisation.company.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a02_organisation.company.dto.CompanyCreateRequest;
import sssm.dev.sssm_backend.a02_organisation.company.dto.CompanyResponse;
import sssm.dev.sssm_backend.a02_organisation.company.dto.CompanyUpdateRequest;
import sssm.dev.sssm_backend.a02_organisation.company.entity.Company;
import sssm.dev.sssm_backend.a02_organisation.company.repository.CompanyDAO;

@Service
@RequiredArgsConstructor
public class CompanyService {

  private final CompanyDAO companyDAO;

  public List<CompanyResponse> getCompanies(
      Optional<String> search, Optional<Integer> page, Optional<Integer> pageSize) {
    int pageNumber = page.orElse(1);
    int pageSizeNumber = pageSize.orElse(10);
    List<Company> companies =
        search
            .map(value -> companyDAO.searchCompaniesByName(value, pageNumber, pageSizeNumber))
            .orElseGet(() -> companyDAO.getAllCompanies(pageNumber, pageSizeNumber));
    return companies.stream().map(this::toResponse).toList();
  }

  public CompanyResponse getCompanyById(String companyId) {
    return companyDAO
        .getCompanyById(companyId)
        .map(this::toResponse)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Entreprise introuvable",
                    "Aucune entreprise ne correspond à l'identifiant " + companyId + ".",
                    "NOT_FOUND"));
  }

  public CompanyResponse createCompany(CompanyCreateRequest request) {
    return toResponse(companyDAO.createCompany(toEntity(request)));
  }

  public CompanyResponse updateCompany(String companyId, CompanyUpdateRequest request) {
    if (companyDAO.getCompanyById(companyId).isEmpty())
      throw new NotFoundException(
          "Entreprise introuvable",
          "Aucune entreprise ne correspond à l'identifiant " + companyId + ".",
          "NOT_FOUND");
    Company company = toEntity(request);
    company.setId(companyId);
    return toResponse(companyDAO.updateCompany(company));
  }

  public void deleteCompany(String companyId) {
    if (companyDAO.getCompanyById(companyId).isPresent()) {
      companyDAO.deleteCompany(companyId);
    }
  }

  private Company toEntity(CompanyCreateRequest request) {
    Company company = new Company();
    company.setName(request.getName());
    company.setLegal_name(request.getLegal_name());
    company.setRegistration_number(request.getRegistration_number());
    company.setTax_number(request.getTax_number());
    company.setEmail(request.getEmail());
    company.setPhone(request.getPhone());
    company.setAddress(request.getAddress());
    company.setCity(request.getCity());
    company.setCountry(request.getCountry());
    company.setLogo_url(request.getLogo_url());
    company.setCurrency_code(request.getCurrency_code());
    return company;
  }

  private Company toEntity(CompanyUpdateRequest request) {
    Company company = new Company();
    company.setName(request.getName());
    company.setLegal_name(request.getLegal_name());
    company.setRegistration_number(request.getRegistration_number());
    company.setTax_number(request.getTax_number());
    company.setEmail(request.getEmail());
    company.setPhone(request.getPhone());
    company.setAddress(request.getAddress());
    company.setCity(request.getCity());
    company.setCountry(request.getCountry());
    company.setLogo_url(request.getLogo_url());
    company.setCurrency_code(request.getCurrency_code());
    return company;
  }

  private CompanyResponse toResponse(Company company) {
    return new CompanyResponse(
        company.getId(),
        company.getName(),
        company.getLegal_name(),
        company.getRegistration_number(),
        company.getTax_number(),
        company.getEmail(),
        company.getPhone(),
        company.getAddress(),
        company.getCity(),
        company.getCountry(),
        company.getLogo_url(),
        company.getCurrency_code(),
        company.getCreated_at(),
        company.getUpdated_at());
  }
}
