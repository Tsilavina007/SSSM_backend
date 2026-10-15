package sssm.dev.sssm_backend.a11_inventory.item_type.dto.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import sssm.dev.sssm_backend.a02_organisation.company.dto.mapper.CompanyRestMapper;
import sssm.dev.sssm_backend.a02_organisation.company.entity.Company;
import sssm.dev.sssm_backend.a02_organisation.company.repository.CompanyDAOImpl;
import sssm.dev.sssm_backend.a11_inventory.item_category.dto.ItemCategoryResponse;
import sssm.dev.sssm_backend.a11_inventory.item_category.dto.mapper.ItemCategoryRestMapper;
import sssm.dev.sssm_backend.a11_inventory.item_category.repository.ItemCategoryDAOImpl;
import sssm.dev.sssm_backend.a11_inventory.item_type.dto.ItemTypeResponse;
import sssm.dev.sssm_backend.a11_inventory.item_type.entity.ItemType;

@Component
public class ItemTypeMapper {
  @Autowired private CompanyDAOImpl companyDAOImpl;
  @Autowired private ItemCategoryDAOImpl itemCategoryDAOImpl;
  @Autowired private CompanyRestMapper companyRestMapper;
  @Autowired private ItemCategoryRestMapper itemCategoryRestMapper;

  public ItemType toRest(ItemTypeResponse rs) {
    Company company = companyDAOImpl.getCompanyById(rs.getCompany_id()).get();
    ItemCategoryResponse itemCategoryResponse =
        itemCategoryDAOImpl.getItemCategoryById(rs.getCategory_id()).get();
    return (new ItemType(
        rs.getId(),
        companyRestMapper.toRest(company),
        itemCategoryRestMapper.toRest(itemCategoryResponse),
        rs.getCode(),
        rs.getName(),
        rs.getDescription(),
        rs.getUnit(),
        rs.getQuantity_total(),
        rs.getMinimum_stock(),
        rs.getStatus(),
        rs.getCreated_at(),
        rs.getUpdated_at()));
  }
}
