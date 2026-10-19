package sssm.dev.sssm_backend.a11_inventory.assignment.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import sssm.dev.sssm_backend.a11_inventory.assignment.dto.AssignmentResponse;

/** Typed persistence boundary for the inventory assignment module. */
public interface AssignmentDAO {
  List<AssignmentResponse> listActive(Integer page, Integer size);

  List<AssignmentResponse> listByEmployee(String id);

  List<AssignmentResponse> listByPost(String id);

  List<AssignmentResponse> list(
      Integer page, Integer size, String search, String where, Object... args);

  Optional<AssignmentResponse> one(String id);

  Object save(String id, Map<String, Object> body, boolean update);

  Map<String, Object> delete(String id);

  List<Map<String, Object>> query(String sql, Object... args);
}
