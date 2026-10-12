package sssm.dev.sssm_backend.a11_inventory.item_type.repository;

import java.util.List;
import java.util.Optional;

import sssm.dev.sssm_backend.a11_inventory.item_type.entity.ItemType;

public interface ItemTypeDAO {
  List<ItemType> getAllItemTypes(int page, int pageSize);

  List<ItemType> searchItemTypesByName(String value, int page, int pageSize);

  Optional<ItemType> getItemTypeById(String id);

  ItemType createItemType(ItemType value);

  ItemType updateItemType(ItemType value);

  void deleteItemType(String id);
}
