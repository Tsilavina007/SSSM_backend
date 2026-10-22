package sssm.dev.sssm_backend.a11_inventory.item.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import sssm.dev.sssm_backend.a11_inventory.stock_balance.service.StockBalanceService;

@Service
@RequiredArgsConstructor
public class ItemService {
  private final ItemDAO itemDAO;
  private final ItemQrCodeRestMapper itemQrCodeRestMapper;
  private final ItemRestMapper itemRestMapper;
  private final StockBalanceService stockBalanceService;

  public List<ItemRest> getItems(
      Optional<String> search,
      Optional<Integer> page,
      Optional<Integer> pageSize,
      Optional<String> warehouseId,
      Optional<String> status) {
    int pageNumber = page.orElse(1);
    int pageSizeNumber = pageSize.orElse(10);
    List<ItemResponse> items =
        itemDAO.getItemsFiltered(
            search.orElse(null),
            warehouseId.orElse(null),
            status.orElse(null),
            pageNumber,
            pageSizeNumber);
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

  @Transactional
  public ItemRest createItem(ItemCreateRequest item) {
    ItemResponse created = itemDAO.createItem(item);
    stockBalanceService.syncItem(null, created);
    return itemRestMapper.toRest(created);
  }

  public ItemBulkCreateResult createItems(List<ItemCreateRequest> items) {
    List<ItemRest> createdItems = new ArrayList<>();
    List<ItemBulkFailure> failedItems = new ArrayList<>();
    for (ItemCreateRequest item : items) {
      try {
        ItemResponse created = itemDAO.createItem(item);
        stockBalanceService.syncItem(null, created);
        createdItems.add(itemRestMapper.toRest(created));
      } catch (ServerException e) {
        failedItems.add(new ItemBulkFailure(item, ErrorMapper.toItemError(e)));
      }
    }
    return new ItemBulkCreateResult(createdItems, failedItems);
  }

  @Transactional
  public ItemRest updateItem(String itemId, ItemUpdateRequest updatedItem) {
    ItemResponse before =
        itemDAO
            .getItemById(itemId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Article introuvable",
                        "Aucun article ne correspond à l'identifiant " + itemId + ".",
                        "NOT_FOUND"));
    ItemResponse updated = itemDAO.updateItem(itemId, updatedItem);
    stockBalanceService.syncItem(before, updated);
    return itemRestMapper.toRest(updated);
  }

  @Transactional
  public void deleteItem(String itemId) {
    // La suppression reste idempotente : un identifiant inconnu renvoie 204 comme avant.
    ItemResponse before = itemDAO.getItemById(itemId).orElse(null);
    if (before != null) {
      stockBalanceService.syncItem(before, null);
    }
    itemDAO.deleteItem(itemId);
  }
}
