package sssm.dev.sssm_backend.a02_organisation.company.repository;

import java.util.List;
import java.util.Optional;

import sssm.dev.sssm_backend.a02_organisation.company.entity.Company;

public interface CompanyDAO {
    List<Company> getAllCompanies(int page, int pageSize);

    List<Company> searchCompaniesByName(String searchName, int page, int pageSize);

    Optional<Company> getCompanyById(String companyId);

    Company createCompany(Company company);

    Company updateCompany(Company company);

    void deleteCompany(String companyId);
}
