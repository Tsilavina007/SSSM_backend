package sssm.dev.sssm_backend.a11_inventory.item.repository;

import java.util.List;
import java.util.Optional;

import sssm.dev.sssm_backend.a11_inventory.item.entity.Item;

public interface ItemDAO {
  List<Item> getAllItems(int page, int pageSize);

  List<Item> searchItemsByName(String value, int page, int pageSize);

  Optional<Item> getItemById(String id);

  Item createItem(Item value);

  Item updateItem(Item value);

  void deleteItem(String id);
}
