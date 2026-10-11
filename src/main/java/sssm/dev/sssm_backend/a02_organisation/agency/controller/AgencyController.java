package sssm.dev.sssm_backend.a02_organisation.agency.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.*;

import sssm.dev.sssm_backend.a02_organisation.agency.entity.Agency;
import sssm.dev.sssm_backend.a02_organisation.agency.service.AgencyService;

@RestController
@RequestMapping("/api/agency")
public class AgencyController {
    private final AgencyService agencyService = new AgencyService();

    @GetMapping
    public List<Agency> getAgencies(@RequestParam Optional<Integer> page,
            @RequestParam Optional<Integer> pageSize, @RequestParam Optional<String> search) {
        return agencyService.getAgencies(search, page, pageSize);
    }
    @GetMapping("/{agencyId}")
    public Optional<Agency> getAgencyById(@PathVariable String agencyId) {
        return agencyService.getAgencyById(agencyId);
    }
    @PostMapping
    public Agency createAgency(@RequestBody Agency agency) { return agencyService.createAgency(agency); }
    @PatchMapping("/{agencyId}")
    public Agency updateAgency(@PathVariable String agencyId, @RequestBody Agency agency) {
        return agencyService.updateAgency(agencyId, agency);
    }
    @DeleteMapping("/{agencyId}")
    public void deleteAgency(@PathVariable String agencyId) { agencyService.deleteAgency(agencyId); }
}
