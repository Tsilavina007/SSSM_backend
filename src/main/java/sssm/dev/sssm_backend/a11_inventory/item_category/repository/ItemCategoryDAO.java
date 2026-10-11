package sssm.dev.sssm_backend.a11_inventory.item_category.repository;

import java.util.*;
import sssm.dev.sssm_backend.a11_inventory.item_category.entity.ItemCategory;

public interface ItemCategoryDAO {
  List<ItemCategory> findAll(int page, int pageSize);

  List<ItemCategory> search(String value, int page, int pageSize);

  Optional<ItemCategory> findById(String id);

  ItemCategory create(ItemCategory value);

  ItemCategory update(ItemCategory value);

  void delete(String id);
}
