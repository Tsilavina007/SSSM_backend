package sssm.dev.sssm_backend.a11_inventory.item_category.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import sssm.dev.sssm_backend.a02_organisation.company.entity.Company;
import sssm.dev.sssm_backend.a02_organisation.company.repository.CompanyDAO;
import sssm.dev.sssm_backend.a02_organisation.company.repository.CompanyDAOImpl;
import sssm.dev.sssm_backend.a11_inventory.item_category.dto.ItemCategoryResponse;
import sssm.dev.sssm_backend.a11_inventory.item_category.entity.ItemCategory;
import sssm.dev.sssm_backend.a11_inventory.item_category.repository.ItemCategoryDAO;
import sssm.dev.sssm_backend.a11_inventory.item_category.repository.ItemCategoryDAOImpl;

public class ItemCategoryService {
  private final ItemCategoryDAO itemCategoryDAO;
  private final CompanyDAO companyDAO;

  public ItemCategoryService() {
    this.itemCategoryDAO = new ItemCategoryDAOImpl();
    this.companyDAO = new CompanyDAOImpl();
  }

  public List<ItemCategory> getItemCategories(
      Optional<String> search, Optional<Integer> page, Optional<Integer> pageSize)
      throws SQLException {
    int pageNumber = page.orElse(1);
    int pageSizeNumber = pageSize.orElse(10);
    return search.isPresent()
        ? mapperListResponseToCategory(
            itemCategoryDAO.searchItemCategoriesByName(search.get(), pageNumber, pageSizeNumber))
        : mapperListResponseToCategory(
            itemCategoryDAO.getAllItemCategories(pageNumber, pageSizeNumber));
  }

  public Optional<ItemCategory> getItemCategoryById(String itemCategoryId) throws SQLException {
    return Optional.of(
        mapperResponseToCategory(itemCategoryDAO.getItemCategoryById(itemCategoryId).get()));
  }

  public ItemCategory createItemCategory(ItemCategoryResponse itemCategoryResponse)
      throws SQLException {
    return mapperResponseToCategory(itemCategoryDAO.createItemCategory(itemCategoryResponse));
  }

  public ItemCategory updateItemCategory(
      String itemCategoryId, ItemCategoryResponse updatedItemCategory) throws SQLException {
    if (itemCategoryDAO.getItemCategoryById(itemCategoryId).isEmpty()) {
      throw new IllegalArgumentException("ItemCategory not found with ID: " + itemCategoryId);
    }
    updatedItemCategory.setId(itemCategoryId);
    return mapperResponseToCategory(itemCategoryDAO.updateItemCategory(updatedItemCategory));
  }

  public void deleteItemCategory(String itemCategoryId) {
    itemCategoryDAO.deleteItemCategory(itemCategoryId);
  }

  private ItemCategory mapperResponseToCategory(ItemCategoryResponse rs) throws SQLException {
    Company c = companyDAO.getCompanyById(rs.getCompany_id()).get();
    return (new ItemCategory(rs.getId(), c, rs.getName(), rs.getDescription()));
  }

  private List<ItemCategory> mapperListResponseToCategory(List<ItemCategoryResponse> items)
      throws SQLException {
    return items.stream()
        .map(
            item -> {
              try {
                return mapperResponseToCategory(item);
              } catch (SQLException e) {
                throw new RuntimeException(e);
              }
            })
        .toList();
  }
}
