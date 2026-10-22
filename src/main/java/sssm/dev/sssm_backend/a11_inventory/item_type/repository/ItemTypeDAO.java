package sssm.dev.sssm_backend.a11_inventory.item_type.repository;

import java.util.List;
import java.util.Optional;

import sssm.dev.sssm_backend.a11_inventory.item_type.dto.ItemTypeCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.item_type.dto.ItemTypeResponse;
import sssm.dev.sssm_backend.a11_inventory.item_type.dto.ItemTypeUpdateRequest;

public interface ItemTypeDAO {
  List<ItemTypeResponse> getAllItemTypes(int page, int pageSize);

  List<ItemTypeResponse> getAllItemTypesByItemCategoryId(
      String itemCategoryId, int page, int pageSize);

  List<ItemTypeResponse> searchItemTypesByName(String value, int page, int pageSize);

  Optional<ItemTypeResponse> getItemTypeById(String id);

  ItemTypeResponse createItemType(ItemTypeCreateRequest value);

  ItemTypeResponse updateItemType(String id, ItemTypeUpdateRequest value);

  void archiveItemType(String id, String archivedBy);

  void restoreItemType(String id);

  void deleteItemType(String id);
}
