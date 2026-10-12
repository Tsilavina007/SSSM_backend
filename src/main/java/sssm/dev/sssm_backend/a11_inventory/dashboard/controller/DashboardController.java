package sssm.dev.sssm_backend.a11_inventory.dashboard.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sssm.dev.sssm_backend.a11_inventory.dashboard.service.DashboardService;

@RestController
@RequestMapping("/api/inventory")
public class DashboardController {
  private final DashboardService service = new DashboardService();

  @GetMapping("/dashboard/summary")
  public Object summary() {
    return service.query(
        "SELECT (SELECT count(*) FROM security.item) AS items,(SELECT count(*) FROM security.warehouse) AS warehouses,(SELECT count(*) FROM security.inventory_operation) AS operations");
  }

  @GetMapping("/alerts/low-stock")
  public Object low() {
    return service.query(
        "SELECT t.*,count(i.id) AS stock_quantity FROM security.item_type t LEFT JOIN security.item i ON i.item_type_id=t.id GROUP BY t.id HAVING count(i.id)<t.minimum_stock");
  }
}
