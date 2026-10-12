package sssm.dev.sssm_backend.a11_inventory.item_type.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import sssm.dev.sssm_backend.a02_organisation.company.entity.Company;
import sssm.dev.sssm_backend.a02_organisation.company.service.CompanyService;
import sssm.dev.sssm_backend.a11_inventory.item_category.entity.ItemCategory;
import sssm.dev.sssm_backend.a11_inventory.item_category.service.ItemCategoryService;
import sssm.dev.sssm_backend.a11_inventory.item_type.dto.ItemTypeCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.item_type.dto.ItemTypeResponse;
import sssm.dev.sssm_backend.a11_inventory.item_type.dto.ItemTypeUpdateRequest;
import sssm.dev.sssm_backend.a11_inventory.item_type.entity.ItemType;
import sssm.dev.sssm_backend.a11_inventory.item_type.repository.ItemTypeDAO;
import sssm.dev.sssm_backend.a11_inventory.item_type.repository.ItemTypeDAOImpl;

public class ItemTypeService {
  private final ItemTypeDAO itemTypeDAO;
  private final CompanyService companyService;
  private final ItemCategoryService itemCategoryService;

  public ItemTypeService() {
    this.itemTypeDAO = new ItemTypeDAOImpl();
    this.companyService = new CompanyService();
    this.itemCategoryService = new ItemCategoryService();
  }

  public List<ItemType> getItemTypes(
      Optional<String> search, Optional<Integer> page, Optional<Integer> pageSize) throws SQLException {
    int pageNumber = page.orElse(1);
    int pageSizeNumber = pageSize.orElse(10);
    return search.isPresent()
        ? mapperListResponseToType(itemTypeDAO.searchItemTypesByName(search.get(), pageNumber, pageSizeNumber))
        : mapperListResponseToType(itemTypeDAO.getAllItemTypes(pageNumber, pageSizeNumber));
  }

  public Optional<ItemType> getItemTypeById(String itemTypeId) throws SQLException {
    return Optional.of(mapperResponseToType(itemTypeDAO.getItemTypeById(itemTypeId).get()));
  }

  public ItemType createItemType(ItemTypeCreateRequest itemType) throws SQLException {
    return mapperResponseToType(itemTypeDAO.createItemType(itemType));
  }

  public ItemType updateItemType(String itemTypeId, ItemTypeUpdateRequest updatedItemType) throws SQLException {
    if (itemTypeDAO.getItemTypeById(itemTypeId).isEmpty()) {
      throw new IllegalArgumentException("ItemType not found with ID: " + itemTypeId);
    }
    return mapperResponseToType(itemTypeDAO.updateItemType(itemTypeId, updatedItemType));
  }

  public void deleteItemType(String itemTypeId) {
    itemTypeDAO.deleteItemType(itemTypeId);
  }

  private ItemType mapperResponseToType(ItemTypeResponse rs) throws SQLException {
    Company c = companyService.getCompanyById(rs.getCompany_id()).get();
    ItemCategory ic = itemCategoryService.getItemCategoryById(rs.getCategory_id()).get();

    return (new ItemType(rs.getId(), c, ic, rs.getCode(), rs.getName(), rs.getDescription(), rs.getUnit(), rs.getQuantity_total(), 
                rs.getMinimum_stock(), rs.getStatus(), rs.getCreated_at(), rs.getUpdated_at()));
  }

  private List<ItemType> mapperListResponseToType(List<ItemTypeResponse> items)
      throws SQLException {
    return items.stream()
        .map(
            item -> {
              try {
                return mapperResponseToType(item);
              } catch (SQLException e) {
                throw new RuntimeException(e);
              }
            })
        .toList();
  }
}
