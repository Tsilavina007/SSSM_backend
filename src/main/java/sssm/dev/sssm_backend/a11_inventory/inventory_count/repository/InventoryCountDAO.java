package sssm.dev.sssm_backend.a11_inventory.inventory_count.repository;

import java.util.List;
import java.util.Map;

/** Typed persistence boundary for the inventory inventory count module. */
public interface InventoryCountDAO {
  List<Map<String, Object>> getResults(String id);

  List<Map<String, Object>> list(
      Integer page, Integer size, String search, String where, Object... args);

  Map<String, Object> one(String id);

  Map<String, Object> save(String id, Map<String, Object> body, boolean update);

  Map<String, Object> delete(String id);

  List<Map<String, Object>> query(String sql, Object... args);
}
