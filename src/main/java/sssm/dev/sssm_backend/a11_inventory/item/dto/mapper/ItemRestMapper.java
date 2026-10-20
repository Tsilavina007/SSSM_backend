package sssm.dev.sssm_backend.a11_inventory.item.dto.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a02_organisation.company.dto.mapper.CompanyRestMapper;
import sssm.dev.sssm_backend.a02_organisation.company.entity.Company;
import sssm.dev.sssm_backend.a02_organisation.company.repository.CompanyDAOImpl;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemResponse;
import sssm.dev.sssm_backend.a11_inventory.item.dto.rest.ItemRest;
import sssm.dev.sssm_backend.a11_inventory.item_category.dto.ItemCategoryResponse;
import sssm.dev.sssm_backend.a11_inventory.item_category.dto.mapper.ItemCategoryRestMapper;
import sssm.dev.sssm_backend.a11_inventory.item_category.dto.rest.ItemCategoryRest;
import sssm.dev.sssm_backend.a11_inventory.item_category.repository.ItemCategoryDAOImpl;
import sssm.dev.sssm_backend.a11_inventory.item_type.dto.ItemTypeResponse;
import sssm.dev.sssm_backend.a11_inventory.item_type.dto.mapper.ItemTypeRestMapper;
import sssm.dev.sssm_backend.a11_inventory.item_type.repository.ItemTypeDAOImpl;
import sssm.dev.sssm_backend.a11_inventory.warehouse.dto.WarehouseResponse;
import sssm.dev.sssm_backend.a11_inventory.warehouse.dto.mapper.WarehouseRestMapper;
import sssm.dev.sssm_backend.a11_inventory.warehouse.dto.rest.WarehouseRest;
import sssm.dev.sssm_backend.a11_inventory.warehouse.repository.WarehouseDAOImpl;

@Component
@RequiredArgsConstructor
public class ItemRestMapper {
  private final CompanyDAOImpl companyDAOImpl;
  private final CompanyRestMapper companyRestMapper;
  private final ItemCategoryDAOImpl itemCategoryDAOImpl;
  private final ItemCategoryRestMapper itemCategoryRestMapper;
  private final ItemTypeDAOImpl itemTypeDAOImpl;
  private final ItemTypeRestMapper itemTypeRestMapper;
  private final WarehouseDAOImpl warehouseDAOImpl;
  private final WarehouseRestMapper warehouseRestMapper;

  public ItemRest toRest(ItemResponse rs) {
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
    ItemTypeResponse itemType =
        itemTypeDAOImpl
            .getItemTypeById(rs.getItem_type_id())
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Type d'article introuvable",
                        "Aucun type d'article ne correspond à l'identifiant "
                            + rs.getItem_type_id()
                            + ".",
                        "NOT_FOUND"));
    ItemCategoryRest category = null;
    if (rs.getCategory_id() != null) {
      ItemCategoryResponse categoryResponse =
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
      category = itemCategoryRestMapper.toRest(categoryResponse);
    }
    WarehouseRest warehouse = null;
    if (rs.getCurrent_warehouse_id() != null) {
      WarehouseResponse warehouseResponse =
          warehouseDAOImpl
              .getWarehouseById(rs.getCurrent_warehouse_id())
              .orElseThrow(
                  () ->
                      new NotFoundException(
                          "Entrepôt introuvable",
                          "Aucun entrepôt ne correspond à l'identifiant "
                              + rs.getCurrent_warehouse_id()
                              + ".",
                          "NOT_FOUND"));
      warehouse = warehouseRestMapper.toRest(warehouseResponse);
    }

    return new ItemRest(
        rs.getId(),
        companyRestMapper.toRest(company),
        category,
        itemTypeRestMapper.toRest(itemType),
        warehouse,
        rs.getQr_code(),
        rs.getDescription(),
        rs.getStatus(),
        rs.getCreated_at(),
        rs.getUpdated_at(),
        rs.getArchived_at(),
        rs.getArchived_by(),
        rs.getLast_inventory_at());
  }
}
