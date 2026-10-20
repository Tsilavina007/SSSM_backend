package sssm.dev.sssm_backend.a02_organisation.company.controller;

import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import sssm.dev.sssm_backend.a02_organisation.company.entity.Company;
import sssm.dev.sssm_backend.a02_organisation.company.service.CompanyService;

@RestController
@RequestMapping("/api/company")
public class CompanyController {
  private final CompanyService companyService;

  CompanyController(CompanyService companyService) {
    this.companyService = companyService;
  }

  @GetMapping
  public ResponseEntity<Object> getCompanies(
      @RequestParam Optional<Integer> page,
      @RequestParam Optional<Integer> pageSize,
      @RequestParam Optional<String> search) {
    List<Company> companies = companyService.getCompanies(search, page, pageSize);
    return ResponseEntity.ok().body(companies);
  }

  @GetMapping("/{companyId}")
  public ResponseEntity<Object> getCompanyById(@PathVariable String companyId) {
    Optional<Company> company = companyService.getCompanyById(companyId);
    return ResponseEntity.ok().body(company);
  }

  @PostMapping
  public ResponseEntity<Object> createCompany(@RequestBody Company company) {
    Company companyRes = companyService.createCompany(company);
    return ResponseEntity.ok().body(companyRes);
  }

  @PatchMapping("/{CompanyId}")
  public ResponseEntity<Object> updateCompany(
      @PathVariable String companyId, @RequestBody Company updatedCompany) {
    Company company = companyService.updateCompany(companyId, updatedCompany);
    return ResponseEntity.ok().body(company);
  }

  @DeleteMapping("/{CompanyId}")
  public ResponseEntity<Object> deleteCompany(@PathVariable String companyId) {
    companyService.deleteCompany(companyId);
    return ResponseEntity.ok().body("Company deleted : " + companyId);
  }
}
