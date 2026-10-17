package sssm.dev.sssm_backend.a11_inventory.item_category.dto.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import sssm.dev.sssm_backend.a02_organisation.company.dto.mapper.CompanyRestMapper;
import sssm.dev.sssm_backend.a02_organisation.company.entity.Company;
import sssm.dev.sssm_backend.a02_organisation.company.repository.CompanyDAOImpl;
import sssm.dev.sssm_backend.a11_inventory.item_category.dto.ItemCategoryResponse;
import sssm.dev.sssm_backend.a11_inventory.item_category.dto.rest.ItemCategoryRest;

@Component
@RequiredArgsConstructor
public class ItemCategoryRestMapper {
  private final CompanyDAOImpl companyDAOImpl;
  private final CompanyRestMapper companyRestMapper;

  public ItemCategoryRest toRest(ItemCategoryResponse rs) {
    Company company = companyDAOImpl.getCompanyById(rs.getCompany_id()).get();
    return (new ItemCategoryRest(
        rs.getId(), companyRestMapper.toRest(company), rs.getName(), rs.getDescription()));
  }
}
