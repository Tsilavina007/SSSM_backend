package sssm.dev.sssm_backend.a11_inventory.report.controller;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sssm.dev.sssm_backend.a00_infrastructure.exception.ClientException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;
import sssm.dev.sssm_backend.a01_common.errors.mapper.ErrorMapper;
import sssm.dev.sssm_backend.a11_inventory.report.service.ReportService;

@RestController
@RequestMapping("/api/inventory/reports")
public class ReportController {
  private final ReportService reportService;

  ReportController(ReportService reportService) {
    this.reportService = reportService;
  }

  @GetMapping("/movements")
  public ResponseEntity<Object> movements(
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer pageSize) {
    try {
      return ResponseEntity.ok().body(reportService.list(page, pageSize, null, null));
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
