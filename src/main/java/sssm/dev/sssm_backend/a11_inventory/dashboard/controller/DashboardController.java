package sssm.dev.sssm_backend.a11_inventory.dashboard.controller;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sssm.dev.sssm_backend.a00_infrastructure.exception.ClientException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;
import sssm.dev.sssm_backend.a01_common.errors.mapper.ErrorMapper;
import sssm.dev.sssm_backend.a11_inventory.dashboard.service.DashboardService;

@RestController
@RequestMapping("/api/inventory")
public class DashboardController {
  private final DashboardService dashboardService;

  DashboardController(DashboardService dashboardService) {
    this.dashboardService = dashboardService;
  }

  @GetMapping("/dashboard/summary")
  public ResponseEntity<Object> summary() {
    try {
      return ResponseEntity.ok().body(dashboardService.getSummary());
    } catch (ClientException e) {
      return ResponseEntity.badRequest().body(Map.of("errors", ErrorMapper.toItemError(e)));
    } catch (NotFoundException e) {
      return ResponseEntity.status(NOT_FOUND).body(Map.of("errors", ErrorMapper.toItemError(e)));
    } catch (ServerException e) {
      return ResponseEntity.internalServerError()
          .body(Map.of("errors", ErrorMapper.toItemError(e)));
    }
  }

  @GetMapping("/alerts/low-stock")
  public ResponseEntity<Object> low() {
    try {
      return ResponseEntity.ok().body(dashboardService.getLowStock());
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
