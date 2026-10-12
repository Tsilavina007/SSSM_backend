package sssm.dev.sssm_backend.a11_inventory.item_type.service;

import java.util.List;
import java.util.Optional;

import sssm.dev.sssm_backend.a11_inventory.item_type.entity.ItemType;
import sssm.dev.sssm_backend.a11_inventory.item_type.repository.ItemTypeDAO;
import sssm.dev.sssm_backend.a11_inventory.item_type.repository.ItemTypeDAOImpl;

public class ItemTypeService {
  private final ItemTypeDAO itemTypeDAO;

  public ItemTypeService() {
    this.itemTypeDAO = new ItemTypeDAOImpl();
  }

  public List<ItemType> getItemTypes(
      Optional<String> search, Optional<Integer> page, Optional<Integer> pageSize) {
    int pageNumber = page.orElse(1);
    int pageSizeNumber = pageSize.orElse(10);
    return search.isPresent()
        ? itemTypeDAO.searchItemTypesByName(search.get(), pageNumber, pageSizeNumber)
        : itemTypeDAO.getAllItemTypes(pageNumber, pageSizeNumber);
  }

  public Optional<ItemType> getItemTypeById(String itemTypeId) {
    return itemTypeDAO.getItemTypeById(itemTypeId);
  }

  public ItemType createItemType(ItemType itemType) {
    return itemTypeDAO.createItemType(itemType);
  }

  public ItemType updateItemType(String itemTypeId, ItemType updatedItemType) {
    if (itemTypeDAO.getItemTypeById(itemTypeId).isEmpty()) {
      throw new IllegalArgumentException("ItemType not found with ID: " + itemTypeId);
    }
    updatedItemType.setId(itemTypeId);
    return itemTypeDAO.updateItemType(updatedItemType);
  }

  public void deleteItemType(String itemTypeId) {
    itemTypeDAO.deleteItemType(itemTypeId);
  }
}
