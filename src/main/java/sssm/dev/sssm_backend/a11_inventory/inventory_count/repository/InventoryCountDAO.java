package sssm.dev.sssm_backend.a11_inventory.inventory_count.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import sssm.dev.sssm_backend.a11_inventory.inventory_count.dto.InventoryCountResponse;
import sssm.dev.sssm_backend.a11_inventory.inventory_count.dto.InventoryCountScanResponse;

/** Typed persistence boundary for the inventory inventory count module. */
public interface InventoryCountDAO {
  List<InventoryCountScanResponse> getResults(String id);

  List<InventoryCountResponse> list(
      Integer page, Integer size, String search, String where, Object... args);

  Optional<InventoryCountResponse> one(String id);

  Object save(String id, Map<String, Object> body, boolean update);

  Map<String, Object> delete(String id);

  List<Map<String, Object>> query(String sql, Object... args);
}
