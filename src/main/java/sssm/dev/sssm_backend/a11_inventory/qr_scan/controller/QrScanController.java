package sssm.dev.sssm_backend.a11_inventory.qr_scan.controller;

import java.util.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sssm.dev.sssm_backend.a11_inventory.qr_scan.service.QrScanService;

@RestController
@RequestMapping("/api/inventory/scans")
public class QrScanController {
  private final QrScanService service = new QrScanService();

  @PostMapping("/resolve")
  public Object resolve(@RequestBody Map<String, Object> b) {
    return service.query("SELECT * FROM security.item WHERE qr_code=?", b.get("qrCode"));
  }
}
