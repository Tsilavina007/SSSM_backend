package sssm.dev.sssm_backend.a11_inventory.item_category.service;

import java.util.List;
import java.util.Optional;

import sssm.dev.sssm_backend.a11_inventory.item_category.entity.ItemCategory;
import sssm.dev.sssm_backend.a11_inventory.item_category.repository.ItemCategoryDAO;
import sssm.dev.sssm_backend.a11_inventory.item_category.repository.ItemCategoryDAOImpl;

public class ItemCategoryService {
  private final ItemCategoryDAO itemCategoryDAO;

  public ItemCategoryService() {
    this.itemCategoryDAO = new ItemCategoryDAOImpl();
  }

  public List<ItemCategory> getItemCategories(
      Optional<String> search, Optional<Integer> page, Optional<Integer> pageSize) {
    int pageNumber = page.orElse(1);
    int pageSizeNumber = pageSize.orElse(10);
    return search.isPresent()
        ? itemCategoryDAO.searchItemCategoriesByName(search.get(), pageNumber, pageSizeNumber)
        : itemCategoryDAO.getAllItemCategories(pageNumber, pageSizeNumber);
  }

  public Optional<ItemCategory> getItemCategoryById(String itemCategoryId) {
    return itemCategoryDAO.getItemCategoryById(itemCategoryId);
  }

  public ItemCategory createItemCategory(ItemCategory itemCategory) {
    return itemCategoryDAO.createItemCategory(itemCategory);
  }

  public ItemCategory updateItemCategory(String itemCategoryId, ItemCategory updatedItemCategory) {
    if (itemCategoryDAO.getItemCategoryById(itemCategoryId).isEmpty()) {
      throw new IllegalArgumentException("ItemCategory not found with ID: " + itemCategoryId);
    }
    updatedItemCategory.setId(itemCategoryId);
    return itemCategoryDAO.updateItemCategory(updatedItemCategory);
  }

  public void deleteItemCategory(String itemCategoryId) {
    itemCategoryDAO.deleteItemCategory(itemCategoryId);
  }
}
