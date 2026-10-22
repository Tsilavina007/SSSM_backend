package sssm.dev.sssm_backend.a11_inventory.item.repository;

import java.util.List;
import java.util.Optional;

import sssm.dev.sssm_backend.a01_common.enums.ItemStatus;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemResponse;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemUpdateRequest;

public interface ItemDAO {
  List<ItemResponse> getAllItems(int page, int pageSize);

  List<ItemResponse> searchItemsByName(String value, int page, int pageSize);

  List<ItemResponse> getItemsByWarehouse(String warehouseId, int page, int pageSize);

  List<ItemResponse> getItemsByStatus(String status, int page, int pageSize);

  List<ItemResponse> getItemsFiltered(
      String search, String warehouseId, String status, int page, int pageSize);

  Optional<ItemResponse> getItemById(String id);

  ItemResponse createItem(ItemCreateRequest value);

  ItemResponse updateItem(String id, ItemUpdateRequest value);

  void updateItemStatus(String id, ItemStatus status);

  void updateItemLocation(String id, String warehouseId);

  void updateLastInventoryAt(String id, java.sql.Timestamp value);

  void archiveItem(String id, String archivedBy);

  void restoreItem(String id);

  void deleteItem(String id);
}
