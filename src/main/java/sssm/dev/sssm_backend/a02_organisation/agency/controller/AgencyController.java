package sssm.dev.sssm_backend.a02_organisation.agency.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import sssm.dev.sssm_backend.a02_organisation.agency.dto.AgencyCreateRequest;
import sssm.dev.sssm_backend.a02_organisation.agency.dto.AgencyResponse;
import sssm.dev.sssm_backend.a02_organisation.agency.dto.AgencyUpdateRequest;
import sssm.dev.sssm_backend.a02_organisation.agency.service.AgencyService;

@RestController
@RequestMapping("/api/v1/agency")
public class AgencyController {
  private final AgencyService agencyService;

  AgencyController(AgencyService agencyService) {
    this.agencyService = agencyService;
  }

  @GetMapping
  public List<AgencyResponse> getAgencies(
      @RequestParam Optional<Integer> page,
      @RequestParam Optional<Integer> pageSize,
      @RequestParam Optional<String> search) {
    return agencyService.getAgencies(search, page, pageSize);
  }

  @GetMapping("/{agencyId}")
  public AgencyResponse getAgencyById(@PathVariable String agencyId) {
    return agencyService.getAgencyById(agencyId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public AgencyResponse createAgency(@Valid @RequestBody AgencyCreateRequest request) {
    return agencyService.createAgency(request);
  }

  @PatchMapping("/{agencyId}")
  public AgencyResponse updateAgency(
      @PathVariable String agencyId, @Valid @RequestBody AgencyUpdateRequest request) {
    return agencyService.updateAgency(agencyId, request);
  }

  @DeleteMapping("/{agencyId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteAgency(@PathVariable String agencyId) {
    agencyService.deleteAgency(agencyId);
  }
}
