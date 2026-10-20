package sssm.dev.sssm_backend.a11_inventory.warehouse.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a11_inventory.warehouse.dto.WarehouseCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.warehouse.dto.WarehouseResponse;
import sssm.dev.sssm_backend.a11_inventory.warehouse.dto.WarehouseUpdateRequest;
import sssm.dev.sssm_backend.a11_inventory.warehouse.dto.mapper.WarehouseMapper;
import sssm.dev.sssm_backend.a11_inventory.warehouse.entity.Warehouse;
import sssm.dev.sssm_backend.a11_inventory.warehouse.repository.WarehouseDAO;

@Service
@RequiredArgsConstructor
public class WarehouseService {
  private final WarehouseDAO warehouseDAO;
  private final WarehouseMapper warehouseMapper;

  public List<Warehouse> getWarehouses(
      Optional<String> search, Optional<Integer> page, Optional<Integer> pageSize) {
    int pageNumber = page.orElse(1);
    int pageSizeNumber = pageSize.orElse(10);
    return search.isPresent()
        ? mapperListResponseToWarehouse(
            warehouseDAO.searchWarehousesByName(search.get(), pageNumber, pageSizeNumber))
        : mapperListResponseToWarehouse(warehouseDAO.getAllWarehouses(pageNumber, pageSizeNumber));
  }

  public Optional<Warehouse> getWarehouseById(String warehouseId) {
    Optional<WarehouseResponse> warehouseResponse = warehouseDAO.getWarehouseById(warehouseId);
    if (warehouseResponse.isEmpty()) {
      throw new NotFoundException("Entrepôt introuvable", "Aucun entrepôt ne correspond à l'identifiant " + warehouseId + ".", "NOT_FOUND");
    }
    return Optional.of(warehouseMapper.toRest(warehouseResponse.get()));
  }

  public Warehouse createWarehouse(WarehouseCreateRequest warehouse) {
    return warehouseMapper.toRest(warehouseDAO.createWarehouse(warehouse));
  }

  public Warehouse updateWarehouse(String warehouseId, WarehouseUpdateRequest updatedWarehouse) {
    if (warehouseDAO.getWarehouseById(warehouseId).isEmpty()) {
      throw new NotFoundException("Entrepôt introuvable", "Aucun entrepôt ne correspond à l'identifiant " + warehouseId + ".", "NOT_FOUND");
    }
    return warehouseMapper.toRest(warehouseDAO.updateWarehouse(warehouseId, updatedWarehouse));
  }

  public void deleteWarehouse(String warehouseId) {
    warehouseDAO.deleteWarehouse(warehouseId);
  }

  private List<Warehouse> mapperListResponseToWarehouse(List<WarehouseResponse> items) {
    return items.stream()
        .map(
            item -> {
              return warehouseMapper.toRest(item);
            })
        .toList();
  }
}
