package sssm.dev.sssm_backend.a11_inventory.item.service;

import java.util.List;
import java.util.Optional;

import sssm.dev.sssm_backend.a11_inventory.item.entity.Item;
import sssm.dev.sssm_backend.a11_inventory.item.repository.ItemDAO;
import sssm.dev.sssm_backend.a11_inventory.item.repository.ItemDAOImpl;

public class ItemService {
  private final ItemDAO itemDAO;

  public ItemService() {
    this.itemDAO = new ItemDAOImpl();
  }

  public List<Item> getItems(
      Optional<String> search, Optional<Integer> page, Optional<Integer> pageSize) {
    int pageNumber = page.orElse(1);
    int pageSizeNumber = pageSize.orElse(10);
    return search.isPresent()
        ? itemDAO.searchItemsByName(search.get(), pageNumber, pageSizeNumber)
        : itemDAO.getAllItems(pageNumber, pageSizeNumber);
  }

  public Optional<Item> getItemById(String itemId) {
    return itemDAO.getItemById(itemId);
  }

  public Item createItem(Item item) {
    return itemDAO.createItem(item);
  }

  public Item updateItem(String itemId, Item updatedItem) {
    if (itemDAO.getItemById(itemId).isEmpty()) {
      throw new IllegalArgumentException("Item not found with ID: " + itemId);
    }
    updatedItem.setId(itemId);
    return itemDAO.updateItem(updatedItem);
  }

  public void deleteItem(String itemId) {
    itemDAO.deleteItem(itemId);
  }
}
