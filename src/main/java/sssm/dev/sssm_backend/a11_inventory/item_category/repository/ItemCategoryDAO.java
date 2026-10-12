package sssm.dev.sssm_backend.a11_inventory.item_category.repository;

import java.util.List;
import java.util.Optional;

import sssm.dev.sssm_backend.a11_inventory.item_category.entity.ItemCategory;

public interface ItemCategoryDAO {
  List<ItemCategory> getAllItemCategories(int page, int pageSize);

  List<ItemCategory> searchItemCategoriesByName(String value, int page, int pageSize);

  Optional<ItemCategory> getItemCategoryById(String id);

  ItemCategory createItemCategory(ItemCategory value);

  ItemCategory updateItemCategory(ItemCategory value);

  void deleteItemCategory(String id);
}
