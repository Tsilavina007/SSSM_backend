package sssm.dev.sssm_backend.a11_inventory.item_type.dto.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a02_organisation.company.dto.mapper.CompanyRestMapper;
import sssm.dev.sssm_backend.a02_organisation.company.entity.Company;
import sssm.dev.sssm_backend.a02_organisation.company.repository.CompanyDAOImpl;
import sssm.dev.sssm_backend.a11_inventory.item_category.dto.ItemCategoryResponse;
import sssm.dev.sssm_backend.a11_inventory.item_category.dto.mapper.ItemCategoryRestMapper;
import sssm.dev.sssm_backend.a11_inventory.item_category.dto.rest.ItemCategoryRest;
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
    Company company =
        companyDAOImpl
            .getCompanyById(rs.getCompany_id())
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Entreprise introuvable",
                        "Aucune entreprise ne correspond à l'identifiant "
                            + rs.getCompany_id()
                            + ".",
                        "NOT_FOUND"));
    ItemCategoryRest category = null;
    if (rs.getCategory_id() != null) {
      ItemCategoryResponse itemCategoryResponse =
          itemCategoryDAOImpl
              .getItemCategoryById(rs.getCategory_id())
              .orElseThrow(
                  () ->
                      new NotFoundException(
                          "Catégorie d'article introuvable",
                          "Aucune catégorie d'article ne correspond à l'identifiant "
                              + rs.getCategory_id()
                              + ".",
                          "NOT_FOUND"));
      category = itemCategoryRestMapper.toRest(itemCategoryResponse);
    }
    return new ItemTypeRest(
        rs.getId(),
        companyRestMapper.toRest(company),
        category,
        rs.getCode(),
        rs.getName(),
        rs.getDescription(),
        rs.getUnit(),
        rs.getQuantity_total(),
        rs.getMinimum_stock(),
        rs.getStatus(),
        rs.getCreated_at(),
        rs.getUpdated_at());
  }
}
