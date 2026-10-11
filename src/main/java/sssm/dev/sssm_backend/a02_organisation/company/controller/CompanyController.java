package sssm.dev.sssm_backend.a02_organisation.company.controller;

import java.util.List;
import java.util.Optional;
import org.springframework.web.bind.annotation.*;
import sssm.dev.sssm_backend.a02_organisation.company.entity.Company;
import sssm.dev.sssm_backend.a02_organisation.company.service.CompanyService;

@RestController
@RequestMapping("/api/company")
public class CompanyController {

  private final CompanyService companyService;

  public CompanyController() {
    this.companyService = new CompanyService();
  }

  @GetMapping
  public List<Company> getCompanies(
      @RequestParam Optional<Integer> page,
      @RequestParam Optional<Integer> pageSize,
      @RequestParam Optional<String> search) {

    return companyService.getCompanies(search, page, pageSize);
  }

  @GetMapping("/{companyId}")
  public Optional<Company> getCompanyById(@PathVariable String companyId) {
    return companyService.getCompanyById(companyId);
  }

  @PostMapping
  public Company createCompany(@RequestBody Company Company) {
    return companyService.createCompany(Company);
  }

  @PatchMapping("/{CompanyId}")
  public Company updateCompany(
      @PathVariable String CompanyId, @RequestBody Company updatedCompany) {
    return companyService.updateCompany(CompanyId, updatedCompany);
  }

  @DeleteMapping("/{CompanyId}")
  public void deleteCompany(@PathVariable String CompanyId) {
    companyService.deleteCompany(CompanyId);
  }
}
