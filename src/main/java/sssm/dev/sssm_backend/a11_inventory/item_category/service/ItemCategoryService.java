package sssm.dev.sssm_backend.a11_inventory.item_category.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sssm.dev.sssm_backend.a00_infrastructure.exception.ConflictException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a03_authentication.auth.security.CurrentUser;
import sssm.dev.sssm_backend.a11_inventory.item_category.dto.ItemCategoryCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.item_category.dto.ItemCategoryResponse;
import sssm.dev.sssm_backend.a11_inventory.item_category.dto.ItemCategoryUpdateRequest;
import sssm.dev.sssm_backend.a11_inventory.item_category.dto.mapper.ItemCategoryRestMapper;
import sssm.dev.sssm_backend.a11_inventory.item_category.dto.rest.ItemCategoryRest;
import sssm.dev.sssm_backend.a11_inventory.item_category.repository.ItemCategoryDAO;

@Service
@RequiredArgsConstructor
public class ItemCategoryService {
  private final ItemCategoryDAO itemCategoryDAO;
  private final ItemCategoryRestMapper itemCategoryRestMapper;
  private final CurrentUser currentUser;

  public List<ItemCategoryRest> getItemCategories(
      Optional<String> search, Optional<Integer> page, Optional<Integer> pageSize) {
    int pageNumber = page.orElse(1);
    int pageSizeNumber = pageSize.orElse(10);
    List<ItemCategoryResponse> categories =
        search
            .map(
                value ->
                    itemCategoryDAO.searchItemCategoriesByName(value, pageNumber, pageSizeNumber))
            .orElseGet(() -> itemCategoryDAO.getAllItemCategories(pageNumber, pageSizeNumber));
    return categories.stream().map(itemCategoryRestMapper::toRest).toList();
  }

  public ItemCategoryRest getItemCategoryById(String itemCategoryId) {
    return itemCategoryDAO
        .getItemCategoryById(itemCategoryId)
        .map(itemCategoryRestMapper::toRest)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Catégorie d'article introuvable",
                    "Aucune catégorie d'article ne correspond à l'identifiant "
                        + itemCategoryId
                        + ".",
                    "NOT_FOUND"));
  }

  @Transactional
  public ItemCategoryRest createItemCategory(ItemCategoryCreateRequest request) {
    return itemCategoryRestMapper.toRest(itemCategoryDAO.createItemCategory(request));
  }

  @Transactional
  public ItemCategoryRest updateItemCategory(
      String itemCategoryId, ItemCategoryUpdateRequest request) {
    if (itemCategoryDAO.getItemCategoryById(itemCategoryId).isEmpty()) {
      throw new NotFoundException(
          "Catégorie d'article introuvable",
          "Aucune catégorie d'article ne correspond à l'identifiant " + itemCategoryId + ".",
          "NOT_FOUND");
    }
    return itemCategoryRestMapper.toRest(
        itemCategoryDAO.updateItemCategory(itemCategoryId, request));
  }

  @Transactional
  public void deleteItemCategory(String itemCategoryId) {
    ItemCategoryResponse existing =
        itemCategoryDAO
            .getItemCategoryById(itemCategoryId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Catégorie d'article introuvable",
                        "Aucune catégorie d'article ne correspond à l'identifiant "
                            + itemCategoryId
                            + ".",
                        "NOT_FOUND"));
    if (existing.getArchived_at() != null) {
      throw new NotFoundException(
          "Catégorie d'article introuvable",
          "Aucune catégorie active ne correspond à l'identifiant " + itemCategoryId + ".",
          "NOT_FOUND");
    }
    itemCategoryDAO.archiveItemCategory(itemCategoryId, currentUserId());
  }

  @Transactional
  public ItemCategoryRest restoreItemCategory(String itemCategoryId) {
    ItemCategoryResponse existing =
        itemCategoryDAO
            .getItemCategoryById(itemCategoryId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Catégorie d'article introuvable",
                        "Aucune catégorie d'article ne correspond à l'identifiant "
                            + itemCategoryId
                            + ".",
                        "NOT_FOUND"));
    if (existing.getArchived_at() == null) {
      throw new ConflictException(
          "Catégorie d'article non archivée",
          "La catégorie " + itemCategoryId + " n'est pas archivée.",
          "ITEM_CATEGORY_NOT_ARCHIVED");
    }
    itemCategoryDAO.restoreItemCategory(itemCategoryId);
    return itemCategoryDAO
        .getItemCategoryById(itemCategoryId)
        .map(itemCategoryRestMapper::toRest)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Catégorie d'article introuvable",
                    "Aucune catégorie d'article ne correspond à l'identifiant "
                        + itemCategoryId
                        + ".",
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
