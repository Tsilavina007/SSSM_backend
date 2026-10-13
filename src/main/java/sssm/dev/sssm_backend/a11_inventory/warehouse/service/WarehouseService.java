package sssm.dev.sssm_backend.a11_inventory.warehouse.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import sssm.dev.sssm_backend.a02_organisation.branch.entity.Branch;
import sssm.dev.sssm_backend.a02_organisation.branch.service.BranchService;
import sssm.dev.sssm_backend.a11_inventory.warehouse.dto.WarehouseCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.warehouse.dto.WarehouseResponse;
import sssm.dev.sssm_backend.a11_inventory.warehouse.dto.WarehouseUpdateRequest;
import sssm.dev.sssm_backend.a11_inventory.warehouse.entity.Warehouse;
import sssm.dev.sssm_backend.a11_inventory.warehouse.repository.WarehouseDAO;
import sssm.dev.sssm_backend.a11_inventory.warehouse.repository.WarehouseDAOImpl;

public class WarehouseService {
  private final WarehouseDAO warehouseDAO;
  private final BranchService branchService;

  public WarehouseService() {
    this.warehouseDAO = new WarehouseDAOImpl();
    this.branchService = new BranchService();
  }

  public List<Warehouse> getWarehouses(
      Optional<String> search, Optional<Integer> page, Optional<Integer> pageSize)
      throws SQLException {
    int pageNumber = page.orElse(1);
    int pageSizeNumber = pageSize.orElse(10);
    return search.isPresent()
        ? mapperListResponseToWarehouse(
            warehouseDAO.searchWarehousesByName(search.get(), pageNumber, pageSizeNumber))
        : mapperListResponseToWarehouse(warehouseDAO.getAllWarehouses(pageNumber, pageSizeNumber));
  }

  public Optional<Warehouse> getWarehouseById(String warehouseId) throws SQLException {
    return Optional.of(mapperResponseToWarehouse(warehouseDAO.getWarehouseById(warehouseId).get()));
  }

  public Warehouse createWarehouse(WarehouseCreateRequest warehouse) throws SQLException {
    return mapperResponseToWarehouse(warehouseDAO.createWarehouse(warehouse));
  }

  public Warehouse updateWarehouse(String warehouseId, WarehouseUpdateRequest updatedWarehouse)
      throws SQLException {
    if (warehouseDAO.getWarehouseById(warehouseId).isEmpty()) {
      throw new IllegalArgumentException("Warehouse not found with ID: " + warehouseId);
    }
    return mapperResponseToWarehouse(warehouseDAO.updateWarehouse(warehouseId, updatedWarehouse));
  }

  public void deleteWarehouse(String warehouseId) throws SQLException {
    warehouseDAO.deleteWarehouse(warehouseId);
  }

  private Warehouse mapperResponseToWarehouse(WarehouseResponse rs) throws SQLException {
    if (rs == null || rs.getBranch_id() == null) {
      return null;
    }
    Branch c = branchService.getBranchById(rs.getBranch_id()).get();
    return (new Warehouse(
        rs.getId(),
        rs.getCompany_id(),
        c,
        rs.getName(),
        rs.getAddress(),
        rs.getActive(),
        rs.getCreated_at(),
        rs.getUpdated_at(),
        rs.getArchived_at()));
  }

  private List<Warehouse> mapperListResponseToWarehouse(List<WarehouseResponse> items)
      throws SQLException {
    return items.stream()
        .map(
            item -> {
              try {
                return mapperResponseToWarehouse(item);
              } catch (SQLException e) {
                throw new RuntimeException(e);
              }
            })
        .toList();
  }
}
