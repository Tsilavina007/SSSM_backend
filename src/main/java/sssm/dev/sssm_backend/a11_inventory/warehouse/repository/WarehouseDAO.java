package sssm.dev.sssm_backend.a11_inventory.warehouse.repository;

import java.util.List;
import java.util.Optional;

import sssm.dev.sssm_backend.a11_inventory.warehouse.entity.Warehouse;

public interface WarehouseDAO {
  List<Warehouse> getAllWarehouses(int page, int pageSize);

  List<Warehouse> searchWarehousesByName(String value, int page, int pageSize);

  Optional<Warehouse> getWarehouseById(String id);

  Warehouse createWarehouse(Warehouse value);

  Warehouse updateWarehouse(Warehouse value);

  void deleteWarehouse(String id);
}
