package sssm.dev.sssm_backend.a11_inventory.operation.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import sssm.dev.sssm_backend.a11_inventory.operation.dto.OperationResponse;

/** Typed persistence boundary for the inventory operation module. */
public interface OperationDAO {
  List<Map<String, Object>> list(
      Integer page, Integer size, String search, String where, Object... args);

  Optional<OperationResponse> one(String id);

  Object save(String id, Map<String, Object> body, boolean update);

  String addLine(
      String operationId, String itemId, String movementId, BigDecimal quantity, String notes);

  Map<String, Object> delete(String id);

  List<Map<String, Object>> query(String sql, Object... args);
}
