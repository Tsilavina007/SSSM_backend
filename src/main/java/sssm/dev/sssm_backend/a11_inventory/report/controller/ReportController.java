package sssm.dev.sssm_backend.a11_inventory.report.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sssm.dev.sssm_backend.a01_common.pagination.PageResponse;
import sssm.dev.sssm_backend.a11_inventory.report.dto.rest.ReportMovementRest;
import sssm.dev.sssm_backend.a11_inventory.report.service.ReportService;

@RestController
@RequestMapping("/api/v1/inventory/reports")
public class ReportController {
  private final ReportService reportService;

  ReportController(ReportService reportService) {
    this.reportService = reportService;
  }

  @GetMapping("/movements")
  public PageResponse<ReportMovementRest> movements(
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer pageSize,
      @RequestParam(required = false) String itemId,
      @RequestParam(required = false) String warehouseId) {
    if (itemId != null && warehouseId != null) {
      return reportService.list(
          page,
          pageSize,
          null,
          "item_id = ? AND warehouse_id = ?",
          java.util.UUID.fromString(itemId),
          java.util.UUID.fromString(warehouseId));
    }
    if (itemId != null) {
      return reportService.list(
          page, pageSize, null, "item_id = ?", java.util.UUID.fromString(itemId));
    }
    if (warehouseId != null) {
      return reportService.list(
          page, pageSize, null, "warehouse_id = ?", java.util.UUID.fromString(warehouseId));
    }
    return reportService.list(page, pageSize, null, null);
  }
}
