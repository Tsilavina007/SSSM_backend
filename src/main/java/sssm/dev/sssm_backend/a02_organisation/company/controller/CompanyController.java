package sssm.dev.sssm_backend.a02_organisation.company.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import sssm.dev.sssm_backend.a00_infrastructure.exception.ClientException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;
import sssm.dev.sssm_backend.a02_organisation.company.entity.Company;
import sssm.dev.sssm_backend.a02_organisation.company.service.CompanyService;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/company")
public class CompanyController {
  @Autowired 
  private CompanyService companyService;

  @GetMapping
  public ResponseEntity<Object> getCompanies(
      @RequestParam Optional<Integer> page,
      @RequestParam Optional<Integer> pageSize,
      @RequestParam Optional<String> search) {
    try {
      List<Company> companies = companyService.getCompanies(search, page, pageSize);
      return ResponseEntity.ok().body(companies);
    } catch (ClientException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    } catch (NotFoundException e) {
      return ResponseEntity.status(NOT_FOUND).body(e.getMessage());
    } catch (ServerException e) {
      return ResponseEntity.internalServerError().body(e.getMessage());
    }
  }

  @GetMapping("/{companyId}")
  public ResponseEntity<Object> getCompanyById(@PathVariable String companyId) {
    try {
      Optional<Company> company = companyService.getCompanyById(companyId);
      return ResponseEntity.ok().body(company);
    } catch (ClientException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    } catch (NotFoundException e) {
      return ResponseEntity.status(NOT_FOUND).body(e.getMessage());
    } catch (ServerException e) {
      return ResponseEntity.internalServerError().body(e.getMessage());
    }
  }

  @PostMapping
  public ResponseEntity<Object> createCompany(@RequestBody Company company) {
    try {
      Company companyRes = companyService.createCompany(company);
      return ResponseEntity.ok().body(companyRes);
    } catch (ClientException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    } catch (NotFoundException e) {
      return ResponseEntity.status(NOT_FOUND).body(e.getMessage());
    } catch (ServerException e) {
      return ResponseEntity.internalServerError().body(e.getMessage());
    }
  }

  @PatchMapping("/{CompanyId}")
  public ResponseEntity<Object> updateCompany(
      @PathVariable String companyId, @RequestBody Company updatedCompany) {
    try {
      Company company = companyService.updateCompany(companyId, updatedCompany);
      return ResponseEntity.ok().body(company);
    } catch (ClientException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    } catch (NotFoundException e) {
      return ResponseEntity.status(NOT_FOUND).body(e.getMessage());
    } catch (ServerException e) {
      return ResponseEntity.internalServerError().body(e.getMessage());
    }
  }

  @DeleteMapping("/{CompanyId}")
  public ResponseEntity<Object> deleteCompany(@PathVariable String companyId) {
    try {
      companyService.deleteCompany(companyId);
      return ResponseEntity.ok().body("Company deleted : " + companyId);
    } catch (ClientException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    } catch (NotFoundException e) {
      return ResponseEntity.status(NOT_FOUND).body(e.getMessage());
    } catch (ServerException e) {
      return ResponseEntity.internalServerError().body(e.getMessage());
    }
  }
}
