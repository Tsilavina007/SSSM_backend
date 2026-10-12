package sssm.dev.sssm_backend.a11_inventory.item_category.repository;

import java.util.List;
import java.util.Optional;

import sssm.dev.sssm_backend.a11_inventory.item_category.dto.ItemCategoryResponse;

public interface ItemCategoryDAO {
  List<ItemCategoryResponse> getAllItemCategories(int page, int pageSize);

  List<ItemCategoryResponse> searchItemCategoriesByName(String value, int page, int pageSize);

  Optional<ItemCategoryResponse> getItemCategoryById(String id);

  ItemCategoryResponse createItemCategory(ItemCategoryResponse value);

  ItemCategoryResponse updateItemCategory(ItemCategoryResponse value);

  void deleteItemCategory(String id);
}
