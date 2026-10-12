package sssm.dev.sssm_backend.a11_inventory.operation.controller;

import java.util.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sssm.dev.sssm_backend.a11_inventory.operation.service.OperationService;

@RestController
@RequestMapping("/api/inventory/operations")
public class OperationController {
  private final OperationService service = new OperationService();

  @PostMapping("/receipt")
  public Object receipt(@RequestBody Map<String, Object> b) {
    return service.save(null, b, false);
  }

  @PostMapping("/issue")
  public Object issue(@RequestBody Map<String, Object> b) {
    return service.save(null, b, false);
  }

  @PostMapping("/transfer")
  public Object transfer(@RequestBody Map<String, Object> b) {
    return service.save(null, b, false);
  }
}
