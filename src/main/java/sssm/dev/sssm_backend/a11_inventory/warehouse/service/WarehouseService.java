package sssm.dev.sssm_backend.a11_inventory.warehouse.service;

import java.util.List;
import java.util.Optional;

import sssm.dev.sssm_backend.a11_inventory.warehouse.entity.Warehouse;
import sssm.dev.sssm_backend.a11_inventory.warehouse.repository.WarehouseDAO;
import sssm.dev.sssm_backend.a11_inventory.warehouse.repository.WarehouseDAOImpl;

public class WarehouseService {
  private final WarehouseDAO warehouseDAO;

  public WarehouseService() {
    this.warehouseDAO = new WarehouseDAOImpl();
  }

  public List<Warehouse> getWarehouses(
      Optional<String> search, Optional<Integer> page, Optional<Integer> pageSize) {
    int pageNumber = page.orElse(1);
    int pageSizeNumber = pageSize.orElse(10);
    return search.isPresent()
        ? warehouseDAO.searchWarehousesByName(search.get(), pageNumber, pageSizeNumber)
        : warehouseDAO.getAllWarehouses(pageNumber, pageSizeNumber);
  }

  public Optional<Warehouse> getWarehouseById(String warehouseId) {
    return warehouseDAO.getWarehouseById(warehouseId);
  }

  public Warehouse createWarehouse(Warehouse warehouse) {
    return warehouseDAO.createWarehouse(warehouse);
  }

  public Warehouse updateWarehouse(String warehouseId, Warehouse updatedWarehouse) {
    if (warehouseDAO.getWarehouseById(warehouseId).isEmpty()) {
      throw new IllegalArgumentException("Warehouse not found with ID: " + warehouseId);
    }
    updatedWarehouse.setId(warehouseId);
    return warehouseDAO.updateWarehouse(updatedWarehouse);
  }

  public void deleteWarehouse(String warehouseId) {
    warehouseDAO.deleteWarehouse(warehouseId);
  }
}
