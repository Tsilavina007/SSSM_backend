package sssm.dev.sssm_backend.a07_contracts.contract_service.controller;

import java.util.List;
import java.util.Optional;
import org.springframework.web.bind.annotation.*;
import sssm.dev.sssm_backend.a07_contracts.contract_service.entity.ContractServiceItem;
import sssm.dev.sssm_backend.a07_contracts.contract_service.service.ContractServiceItemService;

@RestController
@RequestMapping("/api/contract-services")
public class ContractServiceController {
  private final ContractServiceItemService service = new ContractServiceItemService();

  @GetMapping("/contract/{contractId}")
  public List<ContractServiceItem> findByContract(@PathVariable String contractId) {
    return service.findByContractId(contractId);
  }

  @GetMapping("/{id}")
  public Optional<ContractServiceItem> findById(@PathVariable String id) {
    return service.findById(id);
  }

  @PostMapping
  public ContractServiceItem create(@RequestBody ContractServiceItem item) {
    return service.create(item);
  }

  @PatchMapping("/{id}")
  public ContractServiceItem update(
      @PathVariable String id, @RequestBody ContractServiceItem item) {
    return service.update(id, item);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable String id) {
    service.delete(id);
  }
}
