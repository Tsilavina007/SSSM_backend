package sssm.dev.sssm_backend.a11_inventory.item_type.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a11_inventory.item_type.dto.ItemTypeCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.item_type.dto.ItemTypeResponse;
import sssm.dev.sssm_backend.a11_inventory.item_type.dto.ItemTypeUpdateRequest;
import sssm.dev.sssm_backend.a11_inventory.item_type.dto.mapper.ItemTypeMapper;
import sssm.dev.sssm_backend.a11_inventory.item_type.entity.ItemType;
import sssm.dev.sssm_backend.a11_inventory.item_type.repository.ItemTypeDAO;

@Service
@RequiredArgsConstructor
public class ItemTypeService {
  private final ItemTypeDAO itemTypeDAO;
  private final ItemTypeMapper itemTypeMapper;

  public List<ItemType> getItemTypes(
      Optional<String> search, Optional<Integer> page, Optional<Integer> pageSize) {
    int pageNumber = page.orElse(1);
    int pageSizeNumber = pageSize.orElse(10);
    return search.isPresent()
        ? mapperListResponseToType(
            itemTypeDAO.searchItemTypesByName(search.get(), pageNumber, pageSizeNumber))
        : mapperListResponseToType(itemTypeDAO.getAllItemTypes(pageNumber, pageSizeNumber));
  }

  public Optional<ItemType> getItemTypeById(String itemTypeId) {
    Optional<ItemTypeResponse> itemTypeResponse = itemTypeDAO.getItemTypeById(itemTypeId);
    if (itemTypeResponse.isEmpty()) {
      throw new NotFoundException("ItemType not found with ID: " + itemTypeId);
    }
    return Optional.of(itemTypeMapper.toRest(itemTypeResponse.get()));
  }

  public List<ItemType> getItemTypesByItemCategoryId(
      String itemCategoryId, Optional<Integer> page, Optional<Integer> pageSize) {
    int pageNumber = page.orElse(1);
    int pageSizeNumber = pageSize.orElse(10);
    return mapperListResponseToType(
        itemTypeDAO.getAllItemTypesByItemCategoryId(itemCategoryId, pageNumber, pageSizeNumber));
  }

  public ItemType createItemType(ItemTypeCreateRequest itemType) {
    return itemTypeMapper.toRest(itemTypeDAO.createItemType(itemType));
  }

  public ItemType updateItemType(String itemTypeId, ItemTypeUpdateRequest updatedItemType) {
    if (itemTypeDAO.getItemTypeById(itemTypeId).isEmpty()) {
      throw new NotFoundException("ItemType not found with ID: " + itemTypeId);
    }
    return itemTypeMapper.toRest(itemTypeDAO.updateItemType(itemTypeId, updatedItemType));
  }

  public void deleteItemType(String itemTypeId) {
    itemTypeDAO.deleteItemType(itemTypeId);
  }

  private List<ItemType> mapperListResponseToType(List<ItemTypeResponse> items) {
    return items.stream()
        .map(
            item -> {
              return itemTypeMapper.toRest(item);
            })
        .toList();
  }
}
