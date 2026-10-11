package sssm.dev.sssm_backend.a07_contracts.contract.controller;

import java.util.List;
import java.util.Optional;
import org.springframework.web.bind.annotation.*;

import sssm.dev.sssm_backend.a07_contracts.contract.entity.Contract;
import sssm.dev.sssm_backend.a07_contracts.contract.service.ContractService;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {
  private final ContractService service = new ContractService();

  @GetMapping
  public List<Contract> findAll(
      @RequestParam Optional<Integer> page, @RequestParam Optional<Integer> pageSize) {
    return service.findAll(page, pageSize);
  }

  @GetMapping("/company/{companyId}")
  public List<Contract> findByCompany(@PathVariable String companyId) {
    return service.findByCompanyId(companyId);
  }

  @GetMapping("/client/{clientId}")
  public List<Contract> findByClient(@PathVariable String clientId) {
    return service.findByClientId(clientId);
  }

  @GetMapping("/{id}")
  public Optional<Contract> findById(@PathVariable String id) {
    return service.findById(id);
  }

  @PostMapping
  public Contract create(@RequestBody Contract contract) {
    return service.create(contract);
  }

  @PatchMapping("/{id}")
  public Contract update(@PathVariable String id, @RequestBody Contract contract) {
    return service.update(id, contract);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable String id) {
    service.delete(id);
  }
}
