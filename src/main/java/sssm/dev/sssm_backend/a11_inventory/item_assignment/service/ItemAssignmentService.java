package sssm.dev.sssm_backend.a11_inventory.item_assignment.service;

import java.util.*;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.entity.ItemAssignment;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.repository.*;

public class ItemAssignmentService {
  private final ItemAssignmentDAO dao = new ItemAssignmentDAOImpl();

  public List<ItemAssignment> findAll(
      Optional<String> q, Optional<Integer> p, Optional<Integer> s) {
    int page = p.orElse(1), size = s.orElse(10);
    return q.isPresent() ? dao.search(q.get(), page, size) : dao.findAll(page, size);
  }

  public Optional<ItemAssignment> findById(String id) {
    return dao.findById(id);
  }

  public ItemAssignment create(ItemAssignment v) {
    return dao.create(v);
  }

  public ItemAssignment update(String id, ItemAssignment v) {
    if (dao.findById(id).isEmpty())
      throw new IllegalArgumentException("ItemAssignment not found with ID: " + id);
    v.setId(id);
    return dao.update(v);
  }

  public void delete(String id) {
    dao.delete(id);
  }
}
