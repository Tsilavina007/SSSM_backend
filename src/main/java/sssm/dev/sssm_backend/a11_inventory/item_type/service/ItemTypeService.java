package sssm.dev.sssm_backend.a11_inventory.item_type.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sssm.dev.sssm_backend.a00_infrastructure.exception.ConflictException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a03_authentication.auth.security.CurrentUser;
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
  private final CurrentUser currentUser;

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
    return itemTypeDAO.getAllItemTypesByItemCategoryId(itemCategoryId, pageNumber, pageSizeNumber)
        .stream()
        .map(itemTypeRestMapper::toRest)
        .toList();
  }

  @Transactional
  public ItemTypeRest createItemType(ItemTypeCreateRequest itemType) {
    return itemTypeRestMapper.toRest(itemTypeDAO.createItemType(itemType));
  }

  @Transactional
  public ItemTypeRest updateItemType(String itemTypeId, ItemTypeUpdateRequest updatedItemType) {
    if (itemTypeDAO.getItemTypeById(itemTypeId).isEmpty()) {
      throw new NotFoundException(
          "Type d'article introuvable",
          "Aucun type d'article ne correspond à l'identifiant " + itemTypeId + ".",
          "NOT_FOUND");
    }
    return itemTypeRestMapper.toRest(itemTypeDAO.updateItemType(itemTypeId, updatedItemType));
  }

  @Transactional
  public void deleteItemType(String itemTypeId) {
    ItemTypeResponse existing =
        itemTypeDAO
            .getItemTypeById(itemTypeId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Type d'article introuvable",
                        "Aucun type d'article ne correspond à l'identifiant " + itemTypeId + ".",
                        "NOT_FOUND"));
    if (existing.getArchived_at() != null) {
      throw new NotFoundException(
          "Type d'article introuvable",
          "Aucun type actif ne correspond à l'identifiant " + itemTypeId + ".",
          "NOT_FOUND");
    }
    itemTypeDAO.archiveItemType(itemTypeId, currentUserId());
  }

  @Transactional
  public ItemTypeRest restoreItemType(String itemTypeId) {
    ItemTypeResponse existing =
        itemTypeDAO
            .getItemTypeById(itemTypeId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Type d'article introuvable",
                        "Aucun type d'article ne correspond à l'identifiant " + itemTypeId + ".",
                        "NOT_FOUND"));
    if (existing.getArchived_at() == null) {
      throw new ConflictException(
          "Type d'article non archivé",
          "Le type " + itemTypeId + " n'est pas archivé.",
          "ITEM_TYPE_NOT_ARCHIVED");
    }
    itemTypeDAO.restoreItemType(itemTypeId);
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

  private String currentUserId() {
    try {
      String userId = currentUser.require().userId();
      if (userId == null) return null;
      UUID.fromString(userId);
      return userId;
    } catch (RuntimeException ignored) {
      return null;
    }
  }
}
