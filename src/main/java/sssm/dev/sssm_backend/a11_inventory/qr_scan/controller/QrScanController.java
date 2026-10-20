package sssm.dev.sssm_backend.a11_inventory.qr_scan.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sssm.dev.sssm_backend.a11_inventory.qr_scan.dto.QrScanRequest;
import sssm.dev.sssm_backend.a11_inventory.qr_scan.dto.rest.QrScanItemRest;
import sssm.dev.sssm_backend.a11_inventory.qr_scan.service.QrScanService;

@RestController
@RequestMapping("/api/inventory/scans")
public class QrScanController {
  private final QrScanService qrScanService;

  QrScanController(QrScanService qrScanService) {
    this.qrScanService = qrScanService;
  }

  @PostMapping("/resolve")
  public List<QrScanItemRest> resolve(@Valid @RequestBody QrScanRequest request) {
    return qrScanService.resolveQrCode(request.qrCode());
  }
}
