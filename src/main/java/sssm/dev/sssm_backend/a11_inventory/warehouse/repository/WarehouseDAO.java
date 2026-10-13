package sssm.dev.sssm_backend.a11_inventory.warehouse.repository;

import java.util.List;
import java.util.Optional;

import sssm.dev.sssm_backend.a11_inventory.warehouse.dto.WarehouseCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.warehouse.dto.WarehouseResponse;
import sssm.dev.sssm_backend.a11_inventory.warehouse.dto.WarehouseUpdateRequest;

public interface WarehouseDAO {
  List<WarehouseResponse> getAllWarehouses(int page, int pageSize);

  List<WarehouseResponse> searchWarehousesByName(String value, int page, int pageSize);

  Optional<WarehouseResponse> getWarehouseById(String id);

  WarehouseResponse createWarehouse(WarehouseCreateRequest value);

  WarehouseResponse updateWarehouse(String id, WarehouseUpdateRequest value);

  void deleteWarehouse(String id);
}
