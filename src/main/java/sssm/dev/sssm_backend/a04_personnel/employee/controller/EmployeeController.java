package sssm.dev.sssm_backend.a04_personnel.employee.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import sssm.dev.sssm_backend.a04_personnel.employee.dto.EmployeeCreateRequest;
import sssm.dev.sssm_backend.a04_personnel.employee.dto.EmployeeResponse;
import sssm.dev.sssm_backend.a04_personnel.employee.dto.EmployeeUpdateRequest;
import sssm.dev.sssm_backend.a04_personnel.employee.service.EmployeeService;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
  private final EmployeeService service;

  EmployeeController(EmployeeService service) {
    this.service = service;
  }

  @GetMapping
  public List<EmployeeResponse> findAll(
      @RequestParam Optional<String> search,
      @RequestParam Optional<Integer> page,
      @RequestParam Optional<Integer> pageSize) {
    return service.findAll(search, page, pageSize);
  }

  @GetMapping("/{id}")
  public EmployeeResponse findById(@PathVariable String id) {
    return service.findById(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public EmployeeResponse create(@Valid @RequestBody EmployeeCreateRequest request) {
    return service.create(request);
  }

  @PatchMapping("/{id}")
  public EmployeeResponse update(
      @PathVariable String id, @Valid @RequestBody EmployeeUpdateRequest request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable String id) {
    service.delete(id);
  }
}
