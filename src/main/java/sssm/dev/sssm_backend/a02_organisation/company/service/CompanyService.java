package sssm.dev.sssm_backend.a02_organisation.company.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a02_organisation.company.entity.Company;
import sssm.dev.sssm_backend.a02_organisation.company.repository.CompanyDAO;

@Service
@RequiredArgsConstructor
public class CompanyService {

  private final CompanyDAO companyDAO;

  public List<Company> getCompanies(
      Optional<String> search, Optional<Integer> page, Optional<Integer> pageSize) {

    int pageNumber = page.orElse(1);
    int pageSizeNumber = pageSize.orElse(10);

    if (search.isPresent()) {
      return companyDAO.searchCompaniesByName(search.get(), pageNumber, pageSizeNumber);
    } else {
      return companyDAO.getAllCompanies(pageNumber, pageSizeNumber);
    }
  }

  public Optional<Company> getCompanyById(String companyId) {
    return companyDAO.getCompanyById(companyId);
  }

  public Company createCompany(Company Company) {
    return companyDAO.createCompany(Company);
  }

  public Company updateCompany(String companyId, Company updatedCompany) {
    if (companyDAO.getCompanyById(companyId).isPresent()) {
      updatedCompany.setId(companyId);
      return companyDAO.updateCompany(updatedCompany);
    } else {
      throw new NotFoundException("Entreprise introuvable", "Aucune entreprise ne correspond à l'identifiant " + companyId + ".", "NOT_FOUND");
    }
  }

  public void deleteCompany(String companyId) {
    if (companyDAO.getCompanyById(companyId).isPresent()) {
      companyDAO.deleteCompany(companyId);
    }
  }
}
