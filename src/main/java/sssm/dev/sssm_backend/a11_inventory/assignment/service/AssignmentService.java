package sssm.dev.sssm_backend.a11_inventory.assignment.service;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a11_inventory.assignment.repository.AssignmentDAO;

@Service
@RequiredArgsConstructor
public class AssignmentService {
  private final AssignmentDAO assignmentDAO;

  public List<Map<String, Object>> listActive(Integer page, Integer size) {
    return assignmentDAO.listActive(page, size);
  }

  public List<Map<String, Object>> listByEmployee(String id) {
    return assignmentDAO.listByEmployee(id);
  }

  public List<Map<String, Object>> listByPost(String id) {
    return assignmentDAO.listByPost(id);
  }

  public List<Map<String, Object>> list(Integer p, Integer s, String q, String w, Object... a) {
    return assignmentDAO.list(p, s, q, w, a);
  }

  public Map<String, Object> one(String id) {
    return assignmentDAO.one(id);
  }

  public Map<String, Object> save(String id, Map<String, Object> b, boolean u) {
    return assignmentDAO.save(id, b, u);
  }

  public Map<String, Object> delete(String id) {
    return assignmentDAO.delete(id);
  }

  public List<Map<String, Object>> query(String q, Object... a) {
    return assignmentDAO.query(q, a);
  }
}
