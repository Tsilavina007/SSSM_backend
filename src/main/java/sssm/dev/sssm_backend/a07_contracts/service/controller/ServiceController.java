package sssm.dev.sssm_backend.a07_contracts.service.controller;

import java.util.List;
import java.util.Optional;
import org.springframework.web.bind.annotation.*;

import sssm.dev.sssm_backend.a07_contracts.service.entity.Service;
import sssm.dev.sssm_backend.a07_contracts.service.service.ServiceService;

@RestController
@RequestMapping("/api/services")
public class ServiceController {
  private final ServiceService service = new ServiceService();

  @GetMapping
  public List<Service> findAll(
      @RequestParam Optional<Integer> page, @RequestParam Optional<Integer> pageSize) {
    return service.findAll(page, pageSize);
  }

  @GetMapping("/company/{companyId}")
  public List<Service> findByCompany(@PathVariable String companyId) {
    return service.findByCompanyId(companyId);
  }

  @GetMapping("/{id}")
  public Optional<Service> findById(@PathVariable String id) {
    return service.findById(id);
  }

  @PostMapping
  public Service create(@RequestBody Service value) {
    return service.create(value);
  }

  @PatchMapping("/{id}")
  public Service update(@PathVariable String id, @RequestBody Service value) {
    return service.update(id, value);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable String id) {
    service.delete(id);
  }
}
