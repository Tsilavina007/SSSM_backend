package sssm.dev.sssm_backend.a11_inventory.item_type.repository;

import java.util.*;
import sssm.dev.sssm_backend.a11_inventory.item_type.entity.ItemType;

public interface ItemTypeDAO {
  List<ItemType> findAll(int page, int pageSize);

  List<ItemType> search(String value, int page, int pageSize);

  Optional<ItemType> findById(String id);

  ItemType create(ItemType value);

  ItemType update(ItemType value);

  void delete(String id);
}
