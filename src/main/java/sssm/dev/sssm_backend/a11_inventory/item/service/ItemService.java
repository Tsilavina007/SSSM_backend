package sssm.dev.sssm_backend.a11_inventory.item.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;
import sssm.dev.sssm_backend.a01_common.errors.mapper.ErrorMapper;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemBulkCreateResult;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemBulkFailure;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemResponse;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemUpdateRequest;
import sssm.dev.sssm_backend.a11_inventory.item.dto.mapper.ItemQrCodeRestMapper;
import sssm.dev.sssm_backend.a11_inventory.item.dto.mapper.ItemRestMapper;
import sssm.dev.sssm_backend.a11_inventory.item.dto.rest.ItemQrCodeRest;
import sssm.dev.sssm_backend.a11_inventory.item.dto.rest.ItemRest;
import sssm.dev.sssm_backend.a11_inventory.item.repository.ItemDAO;

@Service
@RequiredArgsConstructor
public class ItemService {
  private final ItemDAO itemDAO;
  private final ItemQrCodeRestMapper itemQrCodeRestMapper;
  private final ItemRestMapper itemRestMapper;

  public List<ItemRest> getItems(
      Optional<String> search, Optional<Integer> page, Optional<Integer> pageSize) {
    int pageNumber = page.orElse(1);
    int pageSizeNumber = pageSize.orElse(10);
    List<ItemResponse> items =
        search
            .map(value -> itemDAO.searchItemsByName(value, pageNumber, pageSizeNumber))
            .orElseGet(() -> itemDAO.getAllItems(pageNumber, pageSizeNumber));
    return items.stream().map(itemRestMapper::toRest).toList();
  }

  public ItemRest getItemById(String itemId) {
    return itemDAO
        .getItemById(itemId)
        .map(itemRestMapper::toRest)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Article introuvable",
                    "Aucun article ne correspond à l'identifiant " + itemId + ".",
                    "NOT_FOUND"));
  }

  public ItemQrCodeRest getQrCodeByItemId(String itemId) {
    return itemDAO
        .getItemById(itemId)
        .map(itemQrCodeRestMapper::toRest)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Article introuvable",
                    "Aucun article ne correspond à l'identifiant " + itemId + ".",
                    "NOT_FOUND"));
  }

  public ItemRest createItem(ItemCreateRequest item) {
    return itemRestMapper.toRest(itemDAO.createItem(item));
  }

  public ItemBulkCreateResult createItems(List<ItemCreateRequest> items) {
    List<ItemRest> createdItems = new ArrayList<>();
    List<ItemBulkFailure> failedItems = new ArrayList<>();
    for (ItemCreateRequest item : items) {
      try {
        createdItems.add(itemRestMapper.toRest(itemDAO.createItem(item)));
      } catch (ServerException e) {
        failedItems.add(new ItemBulkFailure(item, ErrorMapper.toItemError(e)));
      }
    }
    return new ItemBulkCreateResult(createdItems, failedItems);
  }

  public ItemRest updateItem(String itemId, ItemUpdateRequest updatedItem) {
    if (itemDAO.getItemById(itemId).isEmpty()) {
      throw new NotFoundException(
          "Article introuvable",
          "Aucun article ne correspond à l'identifiant " + itemId + ".",
          "NOT_FOUND");
    }
    return itemRestMapper.toRest(itemDAO.updateItem(itemId, updatedItem));
  }

  public void deleteItem(String itemId) {
    itemDAO.deleteItem(itemId);
  }
}
