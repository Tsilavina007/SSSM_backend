package sssm.dev.sssm_backend.a11_inventory.item.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;
import sssm.dev.sssm_backend.a01_common.errors.ItemError;
import sssm.dev.sssm_backend.a01_common.errors.mapper.ErrorMapper;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemResponse;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemUpdateRequest;
import sssm.dev.sssm_backend.a11_inventory.item.dto.mapper.ItemMapper;
import sssm.dev.sssm_backend.a11_inventory.item.dto.mapper.ItemQrCodeRestMapper;
import sssm.dev.sssm_backend.a11_inventory.item.dto.rest.ItemQrCodeRest;
import sssm.dev.sssm_backend.a11_inventory.item.entity.Item;
import sssm.dev.sssm_backend.a11_inventory.item.repository.ItemDAO;

@Service
@RequiredArgsConstructor
public class ItemService {
  private final ItemDAO itemDAO;
  private final ItemQrCodeRestMapper itemQrCodeRestMapper;
  private final ItemMapper itemMapper;

  public List<Item> getItems(
      Optional<String> search, Optional<Integer> page, Optional<Integer> pageSize) {
    int pageNumber = page.orElse(1);
    int pageSizeNumber = pageSize.orElse(10);
    return search.isPresent()
        ? mapperListResponseToItem(
            itemDAO.searchItemsByName(search.get(), pageNumber, pageSizeNumber))
        : mapperListResponseToItem(itemDAO.getAllItems(pageNumber, pageSizeNumber));
  }

  public Optional<Item> getItemById(String itemId) {
    Optional<ItemResponse> itemResponse = itemDAO.getItemById(itemId);
    if (itemResponse.isEmpty()) {
      throw new NotFoundException("Article introuvable", "Aucun article ne correspond à l'identifiant " + itemId + ".", "NOT_FOUND");
    }
    return Optional.of(itemMapper.toRest(itemResponse.get()));
  }

  public Optional<ItemQrCodeRest> getQrCodeByItemId(String itemId) {
    Optional<ItemResponse> itemResponse = itemDAO.getItemById(itemId);
    if (itemResponse.isEmpty()) {
      throw new NotFoundException("Article introuvable", "Aucun article ne correspond à l'identifiant " + itemId + ".", "NOT_FOUND");
    }
    return Optional.of(itemQrCodeRestMapper.toRest(itemResponse.get()));
  }

  public Item createItem(ItemCreateRequest item) {
    return itemMapper.toRest(itemDAO.createItem(item));
  }

  public List<Object> createItems(List<ItemCreateRequest> items) {
    List<Item> createdItems = new ArrayList<>();
    Map<ItemCreateRequest, ItemError> failedItemsWithErrors = new java.util.HashMap<>();
    for (ItemCreateRequest item : items) {
      try {
        ItemResponse createdItemResponse = itemDAO.createItem(item);
        createdItems.add(itemMapper.toRest(createdItemResponse));
      } catch (ServerException e) {
        failedItemsWithErrors.put(item, ErrorMapper.toItemError(e));
      }
    }
    return new ArrayList<>(List.of(createdItems, failedItemsWithErrors));
  }

  public Item updateItem(String itemId, ItemUpdateRequest updatedItem) {
    Optional<ItemResponse> itemResponse = itemDAO.getItemById(itemId);
    if (itemResponse.isEmpty()) {
      throw new NotFoundException("Article introuvable", "Aucun article ne correspond à l'identifiant " + itemId + ".", "NOT_FOUND");
    }
    return itemMapper.toRest(itemDAO.updateItem(itemId, updatedItem));
  }

  public void deleteItem(String itemId) {
    itemDAO.deleteItem(itemId);
  }

  private List<Item> mapperListResponseToItem(List<ItemResponse> items) {
    return items.stream()
        .map(
            item -> {
              return itemMapper.toRest(item);
            })
        .toList();
  }
}
