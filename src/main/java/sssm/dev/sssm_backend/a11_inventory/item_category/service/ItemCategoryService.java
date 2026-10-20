package sssm.dev.sssm_backend.a11_inventory.item_category.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
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

  public List<ItemCategoryRest> getItemCategories(
      Optional<String> search, Optional<Integer> page, Optional<Integer> pageSize) {
    int pageNumber = page.orElse(1);
    int pageSizeNumber = pageSize.orElse(10);
    List<ItemCategoryResponse> categories =
        search
            .map(
                value ->
                    itemCategoryDAO.searchItemCategoriesByName(value, pageNumber, pageSizeNumber))
            .orElseGet(
                () -> itemCategoryDAO.getAllItemCategories(pageNumber, pageSizeNumber));
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

  public ItemCategoryRest createItemCategory(ItemCategoryCreateRequest request) {
    return itemCategoryRestMapper.toRest(itemCategoryDAO.createItemCategory(request));
  }

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

  public void deleteItemCategory(String itemCategoryId) {
    itemCategoryDAO.deleteItemCategory(itemCategoryId);
  }
}
