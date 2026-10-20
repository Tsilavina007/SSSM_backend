package sssm.dev.sssm_backend.a11_inventory.report.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sssm.dev.sssm_backend.a11_inventory.report.dto.rest.ReportMovementRest;
import sssm.dev.sssm_backend.a11_inventory.report.service.ReportService;

@RestController
@RequestMapping("/api/inventory/reports")
public class ReportController {
  private final ReportService reportService;

  ReportController(ReportService reportService) {
    this.reportService = reportService;
  }

  @GetMapping("/movements")
  public List<ReportMovementRest> movements(
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer pageSize) {
    return reportService.list(page, pageSize, null, null);
  }
}
