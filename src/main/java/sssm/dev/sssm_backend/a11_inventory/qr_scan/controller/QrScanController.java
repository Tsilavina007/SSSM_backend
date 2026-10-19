package sssm.dev.sssm_backend.a11_inventory.qr_scan.controller;

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
import sssm.dev.sssm_backend.a11_inventory.qr_scan.dto.QrScanRequest;
import sssm.dev.sssm_backend.a11_inventory.qr_scan.service.QrScanService;

@RestController
@RequestMapping("/api/inventory/scans")
public class QrScanController {
  private final QrScanService qrScanService;

  QrScanController(QrScanService qrScanService) {
    this.qrScanService = qrScanService;
  }

  @PostMapping("/resolve")
  public ResponseEntity<Object> resolve(@RequestBody QrScanRequest request) {
    try {
      return ResponseEntity.ok().body(qrScanService.resolveQrCode(request.qrCode()));
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
