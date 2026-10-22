package sssm.dev.sssm_backend.a11_inventory.qr_scan.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sssm.dev.sssm_backend.a11_inventory.qr_scan.dto.QrScanRequest;
import sssm.dev.sssm_backend.a11_inventory.qr_scan.dto.rest.QrScanRest;
import sssm.dev.sssm_backend.a11_inventory.qr_scan.service.QrScanService;

/**
 * Résolution des QR codes.
 *
 * <p>La résolution est limitée à l'entreprise de l'appelant et renvoie un article enrichi (type,
 * catégorie, entrepôt, affectation active et détenteur). Un code inconnu ou appartenant à une
 * autre entreprise renvoie 404. Chaque tentative est journalisée.
 */
@RestController
@RequestMapping("/api/inventory/scans")
public class QrScanController {
  private final QrScanService qrScanService;

  QrScanController(QrScanService qrScanService) {
    this.qrScanService = qrScanService;
  }

  @PostMapping("/resolve")
  public QrScanRest resolve(@Valid @RequestBody QrScanRequest request) {
    return qrScanService.resolveQrCode(request.qrCode(), request.deviceId());
  }
}
