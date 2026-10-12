package sssm.dev.sssm_backend.a11_inventory.item_category.repository;

import java.util.List;
import java.util.Optional;

import sssm.dev.sssm_backend.a11_inventory.item_category.dto.ItemCategoryCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.item_category.dto.ItemCategoryResponse;
import sssm.dev.sssm_backend.a11_inventory.item_category.dto.ItemCategoryUpdateRequest;

public interface ItemCategoryDAO {
  List<ItemCategoryResponse> getAllItemCategories(int page, int pageSize);

  List<ItemCategoryResponse> searchItemCategoriesByName(String value, int page, int pageSize);

  Optional<ItemCategoryResponse> getItemCategoryById(String id);

  ItemCategoryResponse createItemCategory(ItemCategoryCreateRequest value);

  ItemCategoryResponse updateItemCategory(String id, ItemCategoryUpdateRequest value);

  void deleteItemCategory(String id);
}
