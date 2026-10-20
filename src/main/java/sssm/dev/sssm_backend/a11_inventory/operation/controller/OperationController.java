package sssm.dev.sssm_backend.a11_inventory.operation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sssm.dev.sssm_backend.a11_inventory.operation.dto.OperationRequest;
import sssm.dev.sssm_backend.a11_inventory.operation.service.OperationService;

@RestController
@RequestMapping("/api/inventory/operations")
public class OperationController {
  private final OperationService operationService;

  OperationController(OperationService operationService) {
    this.operationService = operationService;
  }

  @PostMapping("/receipt")
  public ResponseEntity<Object> receipt(@RequestBody OperationRequest request) {
    return save(request);
  }

  @PostMapping("/issue")
  public ResponseEntity<Object> issue(@RequestBody OperationRequest request) {
    return save(request);
  }

  @PostMapping("/transfer")
  public ResponseEntity<Object> transfer(@RequestBody OperationRequest request) {
    return save(request);
  }

  private ResponseEntity<Object> save(OperationRequest request) {
    return ResponseEntity.ok().body(operationService.save(null, request.toMap(), false));
  }
}
