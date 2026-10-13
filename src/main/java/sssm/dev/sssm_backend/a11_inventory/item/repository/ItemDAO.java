package sssm.dev.sssm_backend.a11_inventory.item.repository;

import java.util.List;
import java.util.Optional;

import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemResponse;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemUpdateRequest;

public interface ItemDAO {
  List<ItemResponse> getAllItems(int page, int pageSize);

  List<ItemResponse> searchItemsByName(String value, int page, int pageSize);

  Optional<ItemResponse> getItemById(String id);

  ItemResponse createItem(ItemCreateRequest value);

  ItemResponse updateItem(String id, ItemUpdateRequest value);

  void deleteItem(String id);
}
