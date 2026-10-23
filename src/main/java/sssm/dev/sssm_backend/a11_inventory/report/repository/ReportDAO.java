package sssm.dev.sssm_backend.a11_inventory.report.repository;

import java.util.List;
import java.util.Map;

import sssm.dev.sssm_backend.a11_inventory.report.dto.ReportMovementResponse;

/** Typed persistence boundary for the inventory report module. */
public interface ReportDAO {
  List<ReportMovementResponse> list(
      Integer page, Integer size, String search, String where, Object... args);

  long countList(String search, String where, Object... args);

  Map<String, Object> one(String id);

  Map<String, Object> save(String id, Map<String, Object> body, boolean update);

  Map<String, Object> delete(String id);

  List<Map<String, Object>> query(String sql, Object... args);
}
