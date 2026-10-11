package sssm.dev.sssm_backend.a02_organisation.company.service;

import java.util.List;
import java.util.Optional;
import sssm.dev.sssm_backend.a02_organisation.company.entity.Company;
import sssm.dev.sssm_backend.a02_organisation.company.repository.CompanyDAO;
import sssm.dev.sssm_backend.a02_organisation.company.repository.CompanyDAOImpl;

public class CompanyService {

  private final CompanyDAO companyDAO;

  public CompanyService() {
    this.companyDAO = new CompanyDAOImpl();
  }

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
      throw new IllegalArgumentException("Company not found with ID: " + companyId);
    }
  }

  public void deleteCompany(String companyId) {
    if (companyDAO.getCompanyById(companyId).isPresent()) {
      companyDAO.deleteCompany(companyId);
    }
  }
}
