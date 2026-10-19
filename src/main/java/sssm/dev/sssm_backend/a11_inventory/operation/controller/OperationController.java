package sssm.dev.sssm_backend.a11_inventory.operation.controller;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sssm.dev.sssm_backend.a00_infrastructure.exception.ClientException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;
import sssm.dev.sssm_backend.a01_common.errors.mapper.ErrorMapper;
import sssm.dev.sssm_backend.a11_inventory.operation.service.OperationService;

@RestController
@RequestMapping("/api/inventory/operations")
public class OperationController {
  private final OperationService operationService;

  OperationController(OperationService operationService) {
    this.operationService = operationService;
  }

  @PostMapping("/receipt")
  public ResponseEntity<Object> receipt(@RequestBody Map<String, Object> b) {
    return save(b);
  }

  @PostMapping("/issue")
  public ResponseEntity<Object> issue(@RequestBody Map<String, Object> b) {
    return save(b);
  }

  @PostMapping("/transfer")
  public ResponseEntity<Object> transfer(@RequestBody Map<String, Object> b) {
    return save(b);
  }

  private ResponseEntity<Object> save(Map<String, Object> b) {
    try {
      return ResponseEntity.ok().body(operationService.save(null, b, false));
    } catch (ClientException e) {
      return ResponseEntity.badRequest().body(Map.of("errors", ErrorMapper.toItemError(e)));
    } catch (NotFoundException e) {
      return ResponseEntity.status(NOT_FOUND).body(Map.of("errors", ErrorMapper.toItemError(e)));
    } catch (ServerException e) {
      return ResponseEntity.internalServerError()
          .body(Map.of("errors", ErrorMapper.toItemError(e)));
    }
  }
}
