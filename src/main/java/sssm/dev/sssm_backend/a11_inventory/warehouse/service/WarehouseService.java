package sssm.dev.sssm_backend.a11_inventory.warehouse.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a11_inventory.warehouse.dto.WarehouseCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.warehouse.dto.WarehouseResponse;
import sssm.dev.sssm_backend.a11_inventory.warehouse.dto.WarehouseUpdateRequest;
import sssm.dev.sssm_backend.a11_inventory.warehouse.dto.mapper.WarehouseRestMapper;
import sssm.dev.sssm_backend.a11_inventory.warehouse.dto.rest.WarehouseRest;
import sssm.dev.sssm_backend.a11_inventory.warehouse.repository.WarehouseDAO;

@Service
@RequiredArgsConstructor
public class WarehouseService {
  private final WarehouseDAO warehouseDAO;
  private final WarehouseRestMapper warehouseRestMapper;

  public List<WarehouseRest> getWarehouses(
      Optional<String> search, Optional<Integer> page, Optional<Integer> pageSize) {
    int pageNumber = page.orElse(1);
    int pageSizeNumber = pageSize.orElse(10);
    List<WarehouseResponse> warehouses =
        search
            .map(value -> warehouseDAO.searchWarehousesByName(value, pageNumber, pageSizeNumber))
            .orElseGet(() -> warehouseDAO.getAllWarehouses(pageNumber, pageSizeNumber));
    return warehouses.stream().map(warehouseRestMapper::toRest).toList();
  }

  public WarehouseRest getWarehouseById(String warehouseId) {
    return warehouseDAO
        .getWarehouseById(warehouseId)
        .map(warehouseRestMapper::toRest)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Entrepôt introuvable",
                    "Aucun entrepôt ne correspond à l'identifiant " + warehouseId + ".",
                    "NOT_FOUND"));
  }

  public WarehouseRest createWarehouse(WarehouseCreateRequest warehouse) {
    return warehouseRestMapper.toRest(warehouseDAO.createWarehouse(warehouse));
  }

  public WarehouseRest updateWarehouse(
      String warehouseId, WarehouseUpdateRequest updatedWarehouse) {
    if (warehouseDAO.getWarehouseById(warehouseId).isEmpty()) {
      throw new NotFoundException(
          "Entrepôt introuvable",
          "Aucun entrepôt ne correspond à l'identifiant " + warehouseId + ".",
          "NOT_FOUND");
    }
    return warehouseRestMapper.toRest(
        warehouseDAO.updateWarehouse(warehouseId, updatedWarehouse));
  }

  public void deleteWarehouse(String warehouseId) {
    warehouseDAO.deleteWarehouse(warehouseId);
  }
}
