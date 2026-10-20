package sssm.dev.sssm_backend.a11_inventory.dashboard.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    return ResponseEntity.ok().body(dashboardService.getSummary());
  }

  @GetMapping("/alerts/low-stock")
  public ResponseEntity<Object> low() {
    return ResponseEntity.ok().body(dashboardService.getLowStock());
  }
}
