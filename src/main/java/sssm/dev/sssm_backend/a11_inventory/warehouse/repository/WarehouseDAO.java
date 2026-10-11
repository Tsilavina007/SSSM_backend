package sssm.dev.sssm_backend.a11_inventory.warehouse.repository;
import java.util.*;
import sssm.dev.sssm_backend.a11_inventory.warehouse.entity.Warehouse;
public interface WarehouseDAO { List<Warehouse> findAll(int page,int pageSize); List<Warehouse> search(String value,int page,int pageSize); Optional<Warehouse> findById(String id); Warehouse create(Warehouse value); Warehouse update(Warehouse value); void delete(String id); }
