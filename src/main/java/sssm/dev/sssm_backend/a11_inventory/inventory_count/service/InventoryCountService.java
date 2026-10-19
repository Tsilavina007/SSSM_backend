package sssm.dev.sssm_backend.a11_inventory.inventory_count.service;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a11_inventory.inventory_count.repository.InventoryCountDAO;

@Service
@RequiredArgsConstructor
public class InventoryCountService {
  private final InventoryCountDAO inventoryCountDAO;

  public List<Map<String, Object>> getResults(String id) {
    return inventoryCountDAO.getResults(id);
  }

  public Map<String, Object> scan(String id, Map<String, Object> b) {
    b.put("inventory_count_id", id);
    return save(null, b, false);
  }

  public Map<String, Object> close(String id, Map<String, Object> b) {
    return save(id, b == null ? Map.of("status", "CLOSED") : b, true);
  }

  public List<Map<String, Object>> list(Integer p, Integer s, String q, String w, Object... a) {
    return inventoryCountDAO.list(p, s, q, w, a);
  }

  public Map<String, Object> one(String id) {
    return inventoryCountDAO.one(id);
  }

  public Map<String, Object> save(String id, Map<String, Object> b, boolean u) {
    return inventoryCountDAO.save(id, b, u);
  }

  public Map<String, Object> delete(String id) {
    return inventoryCountDAO.delete(id);
  }

  public List<Map<String, Object>> query(String q, Object... a) {
    return inventoryCountDAO.query(q, a);
  }
}
