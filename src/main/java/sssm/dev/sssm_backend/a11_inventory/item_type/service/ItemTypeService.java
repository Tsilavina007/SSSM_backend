package sssm.dev.sssm_backend.a11_inventory.item_type.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a11_inventory.item_type.dto.ItemTypeCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.item_type.dto.ItemTypeResponse;
import sssm.dev.sssm_backend.a11_inventory.item_type.dto.ItemTypeUpdateRequest;
import sssm.dev.sssm_backend.a11_inventory.item_type.dto.mapper.ItemTypeRestMapper;
import sssm.dev.sssm_backend.a11_inventory.item_type.dto.rest.ItemTypeRest;
import sssm.dev.sssm_backend.a11_inventory.item_type.repository.ItemTypeDAO;

@Service
@RequiredArgsConstructor
public class ItemTypeService {
  private final ItemTypeDAO itemTypeDAO;
  private final ItemTypeRestMapper itemTypeRestMapper;

  public List<ItemTypeRest> getItemTypes(
      Optional<String> search, Optional<Integer> page, Optional<Integer> pageSize) {
    int pageNumber = page.orElse(1);
    int pageSizeNumber = pageSize.orElse(10);
    List<ItemTypeResponse> types =
        search
            .map(value -> itemTypeDAO.searchItemTypesByName(value, pageNumber, pageSizeNumber))
            .orElseGet(() -> itemTypeDAO.getAllItemTypes(pageNumber, pageSizeNumber));
    return types.stream().map(itemTypeRestMapper::toRest).toList();
  }

  public ItemTypeRest getItemTypeById(String itemTypeId) {
    return itemTypeDAO
        .getItemTypeById(itemTypeId)
        .map(itemTypeRestMapper::toRest)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Type d'article introuvable",
                    "Aucun type d'article ne correspond à l'identifiant " + itemTypeId + ".",
                    "NOT_FOUND"));
  }

  public List<ItemTypeRest> getItemTypesByItemCategoryId(
      String itemCategoryId, Optional<Integer> page, Optional<Integer> pageSize) {
    int pageNumber = page.orElse(1);
    int pageSizeNumber = pageSize.orElse(10);
    return itemTypeDAO
        .getAllItemTypesByItemCategoryId(itemCategoryId, pageNumber, pageSizeNumber)
        .stream()
        .map(itemTypeRestMapper::toRest)
        .toList();
  }

  public ItemTypeRest createItemType(ItemTypeCreateRequest itemType) {
    return itemTypeRestMapper.toRest(itemTypeDAO.createItemType(itemType));
  }

  public ItemTypeRest updateItemType(String itemTypeId, ItemTypeUpdateRequest updatedItemType) {
    if (itemTypeDAO.getItemTypeById(itemTypeId).isEmpty()) {
      throw new NotFoundException(
          "Type d'article introuvable",
          "Aucun type d'article ne correspond à l'identifiant " + itemTypeId + ".",
          "NOT_FOUND");
    }
    return itemTypeRestMapper.toRest(itemTypeDAO.updateItemType(itemTypeId, updatedItemType));
  }

  public void deleteItemType(String itemTypeId) {
    itemTypeDAO.deleteItemType(itemTypeId);
  }
}
