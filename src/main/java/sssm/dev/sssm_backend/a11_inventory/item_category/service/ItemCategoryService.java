package sssm.dev.sssm_backend.a11_inventory.item_category.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a02_organisation.company.entity.Company;
import sssm.dev.sssm_backend.a02_organisation.company.service.CompanyService;
import sssm.dev.sssm_backend.a11_inventory.item_category.dto.ItemCategoryCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.item_category.dto.ItemCategoryResponse;
import sssm.dev.sssm_backend.a11_inventory.item_category.dto.ItemCategoryUpdateRequest;
import sssm.dev.sssm_backend.a11_inventory.item_category.entity.ItemCategory;
import sssm.dev.sssm_backend.a11_inventory.item_category.repository.ItemCategoryDAO;

@Service
@RequiredArgsConstructor
public class ItemCategoryService {
  private final ItemCategoryDAO itemCategoryDAO;
  private final CompanyService companyService;

  public List<ItemCategory> getItemCategories(
      Optional<String> search, Optional<Integer> page, Optional<Integer> pageSize) {
    int pageNumber = page.orElse(1);
    int pageSizeNumber = pageSize.orElse(10);
    return search.isPresent()
        ? mapperListResponseToCategory(
            itemCategoryDAO.searchItemCategoriesByName(search.get(), pageNumber, pageSizeNumber))
        : mapperListResponseToCategory(
            itemCategoryDAO.getAllItemCategories(pageNumber, pageSizeNumber));
  }

  public Optional<ItemCategory> getItemCategoryById(String itemCategoryId) {
    return Optional.of(
        mapperResponseToCategory(itemCategoryDAO.getItemCategoryById(itemCategoryId).get()));
  }

  public ItemCategory createItemCategory(ItemCategoryCreateRequest itemCategoryCreateRequest) {
    return mapperResponseToCategory(itemCategoryDAO.createItemCategory(itemCategoryCreateRequest));
  }

  public ItemCategory updateItemCategory(
      String itemCategoryId, ItemCategoryUpdateRequest updatedItemCategory) {
    if (itemCategoryDAO.getItemCategoryById(itemCategoryId).isEmpty()) {
      throw new NotFoundException("ItemCategory not found with ID: " + itemCategoryId);
    }
    ;
    return mapperResponseToCategory(
        itemCategoryDAO.updateItemCategory(itemCategoryId, updatedItemCategory));
  }

  public void deleteItemCategory(String itemCategoryId) {
    itemCategoryDAO.deleteItemCategory(itemCategoryId);
  }

  private ItemCategory mapperResponseToCategory(ItemCategoryResponse rs) {
    if (rs == null || rs.getCompany_id() == null) {
      return null;
    }
    Company c = companyService.getCompanyById(rs.getCompany_id()).get();
    return (new ItemCategory(rs.getId(), c, rs.getName(), rs.getDescription()));
  }

  private List<ItemCategory> mapperListResponseToCategory(List<ItemCategoryResponse> items) {
    return items.stream()
        .map(
            item -> {
              return mapperResponseToCategory(item);
            })
        .toList();
  }
}
