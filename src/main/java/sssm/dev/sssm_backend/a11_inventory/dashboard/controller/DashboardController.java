package sssm.dev.sssm_backend.a11_inventory.dashboard.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sssm.dev.sssm_backend.a11_inventory.dashboard.dto.rest.DashboardLowStockRest;
import sssm.dev.sssm_backend.a11_inventory.dashboard.dto.rest.DashboardSummaryRest;
import sssm.dev.sssm_backend.a11_inventory.dashboard.service.DashboardService;

@RestController
@RequestMapping("/api/inventory")
public class DashboardController {
  private final DashboardService dashboardService;

  DashboardController(DashboardService dashboardService) {
    this.dashboardService = dashboardService;
  }

  @GetMapping("/dashboard/summary")
  public List<DashboardSummaryRest> summary() {
    return dashboardService.getSummary();
  }

  @GetMapping("/alerts/low-stock")
  public List<DashboardLowStockRest> low() {
    return dashboardService.getLowStock();
  }
}
