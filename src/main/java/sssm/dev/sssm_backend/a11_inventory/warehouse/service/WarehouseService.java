package sssm.dev.sssm_backend.a11_inventory.warehouse.service;

import java.util.*;
import sssm.dev.sssm_backend.a11_inventory.warehouse.entity.Warehouse;
import sssm.dev.sssm_backend.a11_inventory.warehouse.repository.*;

public class WarehouseService {
  private final WarehouseDAO dao = new WarehouseDAOImpl();

  public List<Warehouse> findAll(Optional<String> q, Optional<Integer> p, Optional<Integer> s) {
    int page = p.orElse(1), size = s.orElse(10);
    return q.isPresent() ? dao.search(q.get(), page, size) : dao.findAll(page, size);
  }

  public Optional<Warehouse> findById(String id) {
    return dao.findById(id);
  }

  public Warehouse create(Warehouse v) {
    return dao.create(v);
  }

  public Warehouse update(String id, Warehouse v) {
    if (dao.findById(id).isEmpty())
      throw new IllegalArgumentException("Warehouse not found with ID: " + id);
    v.setId(id);
    return dao.update(v);
  }

  public void delete(String id) {
    dao.delete(id);
  }
}
