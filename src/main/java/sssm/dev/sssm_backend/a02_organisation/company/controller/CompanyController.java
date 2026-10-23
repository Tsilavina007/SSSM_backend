package sssm.dev.sssm_backend.a02_organisation.company.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import sssm.dev.sssm_backend.a02_organisation.company.dto.CompanyCreateRequest;
import sssm.dev.sssm_backend.a02_organisation.company.dto.CompanyResponse;
import sssm.dev.sssm_backend.a02_organisation.company.dto.CompanyUpdateRequest;
import sssm.dev.sssm_backend.a02_organisation.company.service.CompanyService;

@RestController
@RequestMapping("/api/v1/company")
public class CompanyController {
  private final CompanyService companyService;

  CompanyController(CompanyService companyService) {
    this.companyService = companyService;
  }

  @GetMapping
  public List<CompanyResponse> getCompanies(
      @RequestParam Optional<Integer> page,
      @RequestParam Optional<Integer> pageSize,
      @RequestParam Optional<String> search) {
    return companyService.getCompanies(search, page, pageSize);
  }

  @GetMapping("/{companyId}")
  public CompanyResponse getCompanyById(@PathVariable String companyId) {
    return companyService.getCompanyById(companyId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CompanyResponse createCompany(@Valid @RequestBody CompanyCreateRequest request) {
    return companyService.createCompany(request);
  }

  @PatchMapping("/{companyId}")
  public CompanyResponse updateCompany(
      @PathVariable String companyId, @Valid @RequestBody CompanyUpdateRequest request) {
    return companyService.updateCompany(companyId, request);
  }

  @DeleteMapping("/{companyId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteCompany(@PathVariable String companyId) {
    companyService.deleteCompany(companyId);
  }
}
