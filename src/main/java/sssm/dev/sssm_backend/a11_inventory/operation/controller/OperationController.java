package sssm.dev.sssm_backend.a11_inventory.operation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sssm.dev.sssm_backend.a11_inventory.operation.dto.OperationRequest;
import sssm.dev.sssm_backend.a11_inventory.operation.service.OperationService;

@RestController
@RequestMapping("/api/v1/inventory/operations")
public class OperationController {
  private final OperationService operationService;

  OperationController(OperationService operationService) {
    this.operationService = operationService;
  }

  @PostMapping("/receipt")
  public ResponseEntity<Object> receipt(@RequestBody OperationRequest request) {
    return create("RECEIPT", request);
  }

  @PostMapping("/issue")
  public ResponseEntity<Object> issue(@RequestBody OperationRequest request) {
    return create("ISSUE", request);
  }

  @PostMapping("/transfer")
  public ResponseEntity<Object> transfer(@RequestBody OperationRequest request) {
    return create("TRANSFER", request);
  }

  private ResponseEntity<Object> create(String operationType, OperationRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(operationService.create(operationType, request));
  }
}
