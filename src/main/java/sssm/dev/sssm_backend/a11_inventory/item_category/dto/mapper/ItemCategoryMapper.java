package sssm.dev.sssm_backend.a11_inventory.item_category.dto.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import sssm.dev.sssm_backend.a02_organisation.company.dto.mapper.CompanyRestMapper;
import sssm.dev.sssm_backend.a02_organisation.company.entity.Company;
import sssm.dev.sssm_backend.a02_organisation.company.repository.CompanyDAOImpl;
import sssm.dev.sssm_backend.a11_inventory.item_category.dto.ItemCategoryResponse;
import sssm.dev.sssm_backend.a11_inventory.item_category.entity.ItemCategory;

@Component
@RequiredArgsConstructor
public class ItemCategoryMapper {
  private final CompanyDAOImpl companyDAOImpl;
  private final CompanyRestMapper companyRestMapper;

  public ItemCategory toRest(ItemCategoryResponse rs) {
    Company company = companyDAOImpl.getCompanyById(rs.getCompany_id()).get();
    return (new ItemCategory(
        rs.getId(),
        companyRestMapper.toRest(company),
        rs.getName(),
        rs.getDescription(),
        rs.getCreated_at(),
        rs.getUpdated_at(),
        rs.getArchived_at()));
  }
}
