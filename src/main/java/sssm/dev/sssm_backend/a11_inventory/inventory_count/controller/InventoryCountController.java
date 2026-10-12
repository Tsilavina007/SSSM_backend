package sssm.dev.sssm_backend.a11_inventory.inventory_count.controller;

import java.util.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sssm.dev.sssm_backend.a11_inventory.inventory_count.service.InventoryCountService;

@RestController
@RequestMapping("/api/inventory")
public class InventoryCountController {
  private final InventoryCountService service = new InventoryCountService();

  @GetMapping("/inventory-counts")
  public Object list(
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer pageSize) {
    return service.list(page, pageSize, null, null);
  }

  @PostMapping("/inventory-counts")
  public Object create(@RequestBody Map<String, Object> b) {
    return service.save(null, b, false);
  }

  @PostMapping("/inventory-counts/{id}/scans")
  public Object scan(@PathVariable String id, @RequestBody Map<String, Object> b) {
    b.put("inventory_count_id", id);
    return service.save(null, b, false);
  }

  @GetMapping("/inventory-counts/{id}/results")
  public Object results(@PathVariable String id) {
    return service.query(
        "SELECT s.*,i.qr_code FROM security.inventory_count_scan s JOIN security.item i ON i.id=s.item_id WHERE s.inventory_count_id=?",
        id);
  }

  @PostMapping("/inventory-counts/{id}/close")
  public Object close(
      @PathVariable String id, @RequestBody(required = false) Map<String, Object> b) {
    return service.save(id, b == null ? Map.of("status", "CLOSED") : b, true);
  }
}
