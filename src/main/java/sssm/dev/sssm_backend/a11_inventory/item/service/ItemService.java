package sssm.dev.sssm_backend.a11_inventory.item.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sssm.dev.sssm_backend.a00_infrastructure.exception.ClientException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ConflictException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;
import sssm.dev.sssm_backend.a01_common.errors.mapper.ErrorMapper;
import sssm.dev.sssm_backend.a03_authentication.auth.security.CurrentUser;
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
import sssm.dev.sssm_backend.a11_inventory.item_assignment.service.ItemAssignmentService;
import sssm.dev.sssm_backend.a11_inventory.stock_balance.service.StockBalanceService;

@Service
@RequiredArgsConstructor
public class ItemService {
  private final ItemDAO itemDAO;
  private final ItemQrCodeRestMapper itemQrCodeRestMapper;
  private final ItemRestMapper itemRestMapper;
  private final StockBalanceService stockBalanceService;
  private final ItemAssignmentService itemAssignmentService;
  private final CurrentUser currentUser;

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
    if (updatedItem != null && updatedItem.getQr_code() != null) {
      // D-05 : le code QR est stable après impression. Modifier un code invalide
      // les étiquettes déjà collées sans laisser de trace.
      throw new ClientException(
          "Code QR immuable",
          "Le code QR d'un article ne peut pas être modifié après sa création.",
          "IMMUTABLE_QR_CODE");
    }
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
    ItemResponse before =
        itemDAO
            .getItemById(itemId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Article introuvable",
                        "Aucun article ne correspond à l'identifiant " + itemId + ".",
                        "NOT_FOUND"));
    if (before.getArchived_at() != null) {
      throw new NotFoundException(
          "Article introuvable",
          "Aucun article actif ne correspond à l'identifiant " + itemId + ".",
          "NOT_FOUND");
    }
    if (itemAssignmentService.hasActiveAssignmentForItem(itemId)) {
      throw new ConflictException(
          "Article affecté",
          "L'article "
              + itemId
              + " possède une affectation active : clôturez-la avant de l'archiver.",
          "ITEM_ASSIGNED");
    }

    stockBalanceService.syncItem(before, null);
    itemDAO.archiveItem(itemId, currentUserId());
  }

  @Transactional
  public ItemRest restoreItem(String itemId) {
    ItemResponse existing =
        itemDAO
            .getItemById(itemId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Article introuvable",
                        "Aucun article ne correspond à l'identifiant " + itemId + ".",
                        "NOT_FOUND"));
    if (existing.getArchived_at() == null) {
      throw new ConflictException(
          "Article non archivé",
          "L'article " + itemId + " n'est pas archivé.",
          "ITEM_NOT_ARCHIVED");
    }

    itemDAO.restoreItem(itemId);
    ItemResponse restored =
        itemDAO
            .getItemById(itemId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Article introuvable",
                        "Aucun article ne correspond à l'identifiant " + itemId + ".",
                        "NOT_FOUND"));
    stockBalanceService.syncItem(null, restored);
    return itemRestMapper.toRest(restored);
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
