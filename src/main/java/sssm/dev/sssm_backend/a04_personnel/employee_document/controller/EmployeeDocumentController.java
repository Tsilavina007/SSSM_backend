package sssm.dev.sssm_backend.a04_personnel.employee_document.controller;

import java.util.*;
import org.springframework.web.bind.annotation.*;

import sssm.dev.sssm_backend.a04_personnel.employee_document.entity.EmployeeDocument;
import sssm.dev.sssm_backend.a04_personnel.employee_document.service.EmployeeDocumentService;

@RestController
@RequestMapping("/api/employee-documents")
public class EmployeeDocumentController {
  private final EmployeeDocumentService service;

  EmployeeDocumentController(EmployeeDocumentService service) {
    this.service = service;
  }

  @GetMapping("/employee/{employeeId}")
  public List<EmployeeDocument> findByEmployeeId(@PathVariable String employeeId) {
    return service.findByEmployeeId(employeeId);
  }

  @GetMapping("/{id}")
  public Optional<EmployeeDocument> findById(@PathVariable String id) {
    return service.findById(id);
  }

  @PostMapping
  public EmployeeDocument create(@RequestBody EmployeeDocument document) {
    return service.create(document);
  }

  @PatchMapping("/{id}")
  public EmployeeDocument update(@PathVariable String id, @RequestBody EmployeeDocument document) {
    return service.update(id, document);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable String id) {
    service.delete(id);
  }
}
