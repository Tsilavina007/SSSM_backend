package sssm.dev.sssm_backend.a04_personnel.employee_document.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import sssm.dev.sssm_backend.a04_personnel.employee_document.dto.EmployeeDocumentCreateRequest;
import sssm.dev.sssm_backend.a04_personnel.employee_document.dto.EmployeeDocumentResponse;
import sssm.dev.sssm_backend.a04_personnel.employee_document.dto.EmployeeDocumentUpdateRequest;
import sssm.dev.sssm_backend.a04_personnel.employee_document.service.EmployeeDocumentService;

@RestController
@RequestMapping("/api/employee-documents")
public class EmployeeDocumentController {
  private final EmployeeDocumentService service;

  EmployeeDocumentController(EmployeeDocumentService service) {
    this.service = service;
  }

  @GetMapping("/employee/{employeeId}")
  public List<EmployeeDocumentResponse> findByEmployeeId(@PathVariable String employeeId) {
    return service.findByEmployeeId(employeeId);
  }

  @GetMapping("/{id}")
  public EmployeeDocumentResponse findById(@PathVariable String id) {
    return service.findById(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public EmployeeDocumentResponse create(
      @Valid @RequestBody EmployeeDocumentCreateRequest request) {
    return service.create(request);
  }

  @PatchMapping("/{id}")
  public EmployeeDocumentResponse update(
      @PathVariable String id, @Valid @RequestBody EmployeeDocumentUpdateRequest request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable String id) {
    service.delete(id);
  }
}
