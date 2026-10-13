package sssm.dev.sssm_backend.a11_inventory.item.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import sssm.dev.sssm_backend.a02_organisation.company.entity.Company;
import sssm.dev.sssm_backend.a02_organisation.company.service.CompanyService;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemResponse;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemUpdateRequest;
import sssm.dev.sssm_backend.a11_inventory.item.entity.Item;
import sssm.dev.sssm_backend.a11_inventory.item.repository.ItemDAO;
import sssm.dev.sssm_backend.a11_inventory.item.repository.ItemDAOImpl;
import sssm.dev.sssm_backend.a11_inventory.item_category.entity.ItemCategory;
import sssm.dev.sssm_backend.a11_inventory.item_category.service.ItemCategoryService;
import sssm.dev.sssm_backend.a11_inventory.item_type.entity.ItemType;
import sssm.dev.sssm_backend.a11_inventory.item_type.service.ItemTypeService;
import sssm.dev.sssm_backend.a11_inventory.warehouse.entity.Warehouse;
import sssm.dev.sssm_backend.a11_inventory.warehouse.service.WarehouseService;

public class ItemService {
  private final ItemDAO itemDAO;
  private final CompanyService companyService;
  private final ItemCategoryService itemCategoryService;
  private final ItemTypeService itemTypeService;
  private final WarehouseService warehouseService;

  public ItemService() {
    this.itemDAO = new ItemDAOImpl();
    this.companyService = new CompanyService();
    this.itemCategoryService = new ItemCategoryService();
    this.itemTypeService = new ItemTypeService();
    this.warehouseService = new WarehouseService();
  }

  public List<Item> getItems(
      Optional<String> search, Optional<Integer> page, Optional<Integer> pageSize) throws SQLException {
    int pageNumber = page.orElse(1);
    int pageSizeNumber = pageSize.orElse(10);
    return search.isPresent()
        ? mapperListResponseToItem(itemDAO.searchItemsByName(search.get(), pageNumber, pageSizeNumber))
        : mapperListResponseToItem(itemDAO.getAllItems(pageNumber, pageSizeNumber));
  }

  public Optional<Item> getItemById(String itemId) throws SQLException {
    Optional<ItemResponse> itemResponse = itemDAO.getItemById(itemId);
    if (itemResponse.isEmpty()) {
      throw new IllegalArgumentException("Item not found with ID: " + itemId);
    }
    return Optional.of(mapperResponseToItem(itemResponse.get()));
  }

  public Item createItem(ItemCreateRequest item) throws SQLException {
    return mapperResponseToItem(itemDAO.createItem(item));
  }
  public Item updateItem(String itemId, ItemUpdateRequest updatedItem) throws SQLException {
    Optional<ItemResponse> itemResponse = itemDAO.getItemById(itemId);
    if (itemResponse.isEmpty()) {
      throw new IllegalArgumentException("Item not found with ID: " + itemId);
    }
    return mapperResponseToItem(itemDAO.updateItem(itemId, updatedItem));
  }

  public void deleteItem(String itemId) {
    itemDAO.deleteItem(itemId);
  }

  private Item mapperResponseToItem(ItemResponse rs) throws SQLException {
    if (rs == null || rs.getCompany_id() == null || rs.getCategory_id() == null || rs.getItem_type_id() == null || rs.getCurrent_warehouse_id() == null) {
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

  private List<Item> mapperListResponseToItem(List<ItemResponse> items)
      throws SQLException {
    return items.stream()
        .map(
            item -> {
              try {
                return mapperResponseToItem(item);
              } catch (SQLException e) {
                throw new RuntimeException(e);
              }
            })
        .toList();
  }

}
