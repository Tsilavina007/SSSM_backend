package sssm.dev.sssm_backend.a11_inventory.inventory_count.service;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a11_inventory.inventory_count.dto.InventoryCountResponse;
import sssm.dev.sssm_backend.a11_inventory.inventory_count.dto.InventoryCountScanResponse;
import sssm.dev.sssm_backend.a11_inventory.inventory_count.dto.mapper.InventoryCountRestMapper;
import sssm.dev.sssm_backend.a11_inventory.inventory_count.dto.mapper.InventoryCountScanRestMapper;
import sssm.dev.sssm_backend.a11_inventory.inventory_count.dto.rest.InventoryCountRest;
import sssm.dev.sssm_backend.a11_inventory.inventory_count.dto.rest.InventoryCountScanRest;
import sssm.dev.sssm_backend.a11_inventory.inventory_count.repository.InventoryCountDAO;

@Service
@RequiredArgsConstructor
public class InventoryCountService {
  private final InventoryCountDAO inventoryCountDAO;
  private final InventoryCountRestMapper inventoryCountRestMapper;
  private final InventoryCountScanRestMapper inventoryCountScanRestMapper;

  public List<InventoryCountScanRest> getResults(String id) {
    return mapperListScanResponseToRest(inventoryCountDAO.getResults(id));
  }

  public Object scan(String id, Map<String, Object> b) {
    b.put("inventory_count_id", id);
    return save(null, b, false);
  }

  public Object close(String id, Map<String, Object> b) {
    return save(id, b == null ? Map.of("status", "CLOSED") : b, true);
  }

  public List<InventoryCountRest> list(Integer p, Integer s, String q, String w, Object... a) {
    return mapperListResponseToRest(inventoryCountDAO.list(p, s, q, w, a));
  }

  public Object one(String id) {
    InventoryCountResponse r = inventoryCountDAO.one(id).orElse(null);
    return r != null ? inventoryCountRestMapper.toRest(r) : Map.of("error", "Not found", "id", id);
  }

  public Object save(String id, Map<String, Object> b, boolean u) {
    Object r = inventoryCountDAO.save(id, b, u);
    return r instanceof InventoryCountResponse rs ? inventoryCountRestMapper.toRest(rs) : r;
  }

  public Map<String, Object> delete(String id) {
    return inventoryCountDAO.delete(id);
  }

  public List<Map<String, Object>> query(String q, Object... a) {
    return inventoryCountDAO.query(q, a);
  }

  private List<InventoryCountRest> mapperListResponseToRest(List<InventoryCountResponse> items) {
    return items.stream()
        .map(
            item -> {
              return inventoryCountRestMapper.toRest(item);
            })
        .toList();
  }

  private List<InventoryCountScanRest> mapperListScanResponseToRest(
      List<InventoryCountScanResponse> items) {
    return items.stream()
        .map(
            item -> {
              return inventoryCountScanRestMapper.toRest(item);
            })
        .toList();
  }
}
