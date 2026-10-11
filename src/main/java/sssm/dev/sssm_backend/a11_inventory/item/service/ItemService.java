package sssm.dev.sssm_backend.a11_inventory.item.service;

import java.util.*;
import sssm.dev.sssm_backend.a11_inventory.item.entity.Item;
import sssm.dev.sssm_backend.a11_inventory.item.repository.*;

public class ItemService {
  private final ItemDAO dao = new ItemDAOImpl();

  public List<Item> findAll(Optional<String> q, Optional<Integer> p, Optional<Integer> s) {
    int page = p.orElse(1), size = s.orElse(10);
    return q.isPresent() ? dao.search(q.get(), page, size) : dao.findAll(page, size);
  }

  public Optional<Item> findById(String id) {
    return dao.findById(id);
  }

  public Item create(Item v) {
    return dao.create(v);
  }

  public Item update(String id, Item v) {
    if (dao.findById(id).isEmpty())
      throw new IllegalArgumentException("Item not found with ID: " + id);
    v.setId(id);
    return dao.update(v);
  }

  public void delete(String id) {
    dao.delete(id);
  }
}
