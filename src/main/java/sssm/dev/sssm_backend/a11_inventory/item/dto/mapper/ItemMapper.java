package sssm.dev.sssm_backend.a11_inventory.item.dto.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import sssm.dev.sssm_backend.a02_organisation.company.dto.mapper.CompanyRestMapper;
import sssm.dev.sssm_backend.a02_organisation.company.entity.Company;
import sssm.dev.sssm_backend.a02_organisation.company.repository.CompanyDAOImpl;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemResponse;
import sssm.dev.sssm_backend.a11_inventory.item.entity.Item;
import sssm.dev.sssm_backend.a11_inventory.item_category.dto.ItemCategoryResponse;
import sssm.dev.sssm_backend.a11_inventory.item_category.dto.mapper.ItemCategoryRestMapper;
import sssm.dev.sssm_backend.a11_inventory.item_category.repository.ItemCategoryDAOImpl;
import sssm.dev.sssm_backend.a11_inventory.item_type.dto.ItemTypeResponse;
import sssm.dev.sssm_backend.a11_inventory.item_type.dto.mapper.ItemTypeRestMapper;
import sssm.dev.sssm_backend.a11_inventory.item_type.repository.ItemTypeDAOImpl;
import sssm.dev.sssm_backend.a11_inventory.warehouse.dto.WarehouseResponse;
import sssm.dev.sssm_backend.a11_inventory.warehouse.dto.mapper.WarehouseRestMapper;
import sssm.dev.sssm_backend.a11_inventory.warehouse.repository.WarehouseDAOImpl;

@Component
@RequiredArgsConstructor
public class ItemMapper {
  private final CompanyDAOImpl companyDAOImpl;
  private final CompanyRestMapper companyRestMapper;
  private final ItemCategoryDAOImpl itemCategoryDAOImpl;
  private final ItemCategoryRestMapper itemCategoryRestMapper;
  private final ItemTypeDAOImpl itemTypeDAOImpl;
  private final ItemTypeRestMapper itemTypeRestMapper;
  private final WarehouseDAOImpl warehouseDAOImpl;
  private final WarehouseRestMapper warehouseRestMapper;

  public Item toRest(ItemResponse rs) {
    Company company = companyDAOImpl.getCompanyById(rs.getCompany_id()).get();
    ItemCategoryResponse category =
        itemCategoryDAOImpl.getItemCategoryById(rs.getCategory_id()).get();
    ItemTypeResponse itemType = itemTypeDAOImpl.getItemTypeById(rs.getItem_type_id()).get();
    WarehouseResponse warehouse =
        warehouseDAOImpl.getWarehouseById(rs.getCurrent_warehouse_id()).get();

    return (new Item(
        rs.getId(),
        companyRestMapper.toRest(company),
        itemCategoryRestMapper.toRest(category),
        itemTypeRestMapper.toRest(itemType),
        warehouseRestMapper.toRest(warehouse),
        rs.getQr_code(),
        rs.getDescription(),
        rs.getStatus(),
        rs.getCreated_at(),
        rs.getUpdated_at(),
        rs.getArchived_at(),
        rs.getArchived_by(),
        rs.getLast_inventory_at()));
  }
}
