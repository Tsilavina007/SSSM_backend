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
import sssm.dev.sssm_backend.a02_organisation.company.entity.Company;
import sssm.dev.sssm_backend.a02_organisation.company.service.CompanyService;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemResponse;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemUpdateRequest;
import sssm.dev.sssm_backend.a11_inventory.item.entity.Item;
import sssm.dev.sssm_backend.a11_inventory.item.repository.ItemDAO;
import sssm.dev.sssm_backend.a11_inventory.item_category.entity.ItemCategory;
import sssm.dev.sssm_backend.a11_inventory.item_category.service.ItemCategoryService;
import sssm.dev.sssm_backend.a11_inventory.item_type.entity.ItemType;
import sssm.dev.sssm_backend.a11_inventory.item_type.service.ItemTypeService;
import sssm.dev.sssm_backend.a11_inventory.warehouse.entity.Warehouse;
import sssm.dev.sssm_backend.a11_inventory.warehouse.service.WarehouseService;

@Service
@RequiredArgsConstructor
public class ItemService {
  private final ItemDAO itemDAO;
  private final CompanyService companyService;
  private final ItemCategoryService itemCategoryService;
  private final ItemTypeService itemTypeService;
  private final WarehouseService warehouseService;

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
      throw new NotFoundException("Item not found with ID: " + itemId);
    }
    return Optional.of(mapperResponseToItem(itemResponse.get()));
  }

  public Item createItem(ItemCreateRequest item) {
    return mapperResponseToItem(itemDAO.createItem(item));
  }

  public List<Object> createItems(List<ItemCreateRequest> items) {
    List<Item> createdItems = new ArrayList<>();
    Map<ItemCreateRequest, ItemError> failedItemsWithErrors = new java.util.HashMap<>();
    for (ItemCreateRequest item : items) {
      try {
        ItemResponse createdItemResponse = itemDAO.createItem(item);
        createdItems.add(mapperResponseToItem(createdItemResponse));
      } catch (ServerException e) {
        failedItemsWithErrors.put(item, ErrorMapper.toItemError(e));
      }
    }
    return new ArrayList<>(List.of(createdItems, failedItemsWithErrors));
  }

  public Item updateItem(String itemId, ItemUpdateRequest updatedItem) {
    Optional<ItemResponse> itemResponse = itemDAO.getItemById(itemId);
    if (itemResponse.isEmpty()) {
      throw new NotFoundException("Item not found with ID: " + itemId);
    }
    return mapperResponseToItem(itemDAO.updateItem(itemId, updatedItem));
  }

  public void deleteItem(String itemId) {
    itemDAO.deleteItem(itemId);
  }

  private Item mapperResponseToItem(ItemResponse rs) {
    if (rs == null
        || rs.getCompany_id() == null
        || rs.getCategory_id() == null
        || rs.getItem_type_id() == null
        || rs.getCurrent_warehouse_id() == null) {
      return null;
    }
    Company c = companyService.getCompanyById(rs.getCompany_id()).get();
    ItemCategory ic = itemCategoryService.getItemCategoryById(rs.getCategory_id()).get();
    ItemType it = itemTypeService.getItemTypeById(rs.getItem_type_id()).get();
    Warehouse w = warehouseService.getWarehouseById(rs.getCurrent_warehouse_id()).get();

    return (new Item(
        rs.getId(),
        c,
        ic,
        it,
        rs.getQr_code(),
        rs.getDescription(),
        rs.getStatus(),
        w,
        rs.getCreated_at(),
        rs.getUpdated_at(),
        rs.getArchived_at(),
        rs.getArchived_by(),
        rs.getLast_inventory_at()));
  }

  private List<Item> mapperListResponseToItem(List<ItemResponse> items) {
    return items.stream()
        .map(
            item -> {
              return mapperResponseToItem(item);
            })
        .toList();
  }
}
