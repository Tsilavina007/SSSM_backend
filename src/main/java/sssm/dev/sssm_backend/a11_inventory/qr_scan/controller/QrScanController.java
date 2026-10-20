package sssm.dev.sssm_backend.a11_inventory.qr_scan.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    return ResponseEntity.ok().body(qrScanService.resolveQrCode(request.qrCode()));
  }
}
