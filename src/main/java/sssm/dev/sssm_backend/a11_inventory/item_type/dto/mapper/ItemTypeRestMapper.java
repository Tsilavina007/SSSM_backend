package sssm.dev.sssm_backend.a11_inventory.item_type.dto.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import sssm.dev.sssm_backend.a02_organisation.company.dto.mapper.CompanyRestMapper;
import sssm.dev.sssm_backend.a02_organisation.company.entity.Company;
import sssm.dev.sssm_backend.a02_organisation.company.repository.CompanyDAOImpl;
import sssm.dev.sssm_backend.a11_inventory.item_category.dto.ItemCategoryResponse;
import sssm.dev.sssm_backend.a11_inventory.item_category.dto.mapper.ItemCategoryRestMapper;
import sssm.dev.sssm_backend.a11_inventory.item_category.repository.ItemCategoryDAOImpl;
import sssm.dev.sssm_backend.a11_inventory.item_type.dto.ItemTypeResponse;
import sssm.dev.sssm_backend.a11_inventory.item_type.dto.rest.ItemTypeRest;

@Component
@RequiredArgsConstructor
public class ItemTypeRestMapper {
  private final CompanyDAOImpl companyDAOImpl;
  private final ItemCategoryDAOImpl itemCategoryDAOImpl;
  private final CompanyRestMapper companyRestMapper;
  private final ItemCategoryRestMapper itemCategoryRestMapper;

  public ItemTypeRest toRest(ItemTypeResponse rs) {
    Company company = companyDAOImpl.getCompanyById(rs.getCompany_id()).get();
    ItemCategoryResponse itemCategoryResponse =
        itemCategoryDAOImpl.getItemCategoryById(rs.getCategory_id()).get();
    return (new ItemTypeRest(
        rs.getId(),
        companyRestMapper.toRest(company),
        itemCategoryRestMapper.toRest(itemCategoryResponse),
        rs.getCode(),
        rs.getName(),
        rs.getDescription()));
  }
}
