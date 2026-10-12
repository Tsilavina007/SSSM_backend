package sssm.dev.sssm_backend.a11_inventory.inventory_count.service;

import java.util.*;

import sssm.dev.sssm_backend.a11_inventory.inventory_count.repository.InventoryCountDAO;
import sssm.dev.sssm_backend.a11_inventory.inventory_count.repository.InventoryCountDAOImpl;

public class InventoryCountService {
  private final InventoryCountDAO dao;

  public InventoryCountService() {
    this.dao = new InventoryCountDAOImpl();
  }

  public List<Map<String, Object>> list(Integer p, Integer s, String q, String w, Object... a) {
    return dao.list(p, s, q, w, a);
  }

  public Map<String, Object> one(String id) {
    return dao.one(id);
  }

  public Map<String, Object> save(String id, Map<String, Object> b, boolean u) {
    return dao.save(id, b, u);
  }

  public Map<String, Object> delete(String id) {
    return dao.delete(id);
  }

  public List<Map<String, Object>> query(String q, Object... a) {
    return dao.query(q, a);
  }
}

