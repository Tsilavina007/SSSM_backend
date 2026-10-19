package sssm.dev.sssm_backend.a11_inventory.operation.service;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a11_inventory.operation.repository.OperationDAO;

@Service
@RequiredArgsConstructor
public class OperationService {
  private final OperationDAO operationDAO;

  public List<Map<String, Object>> list(Integer p, Integer s, String q, String w, Object... a) {
    return operationDAO.list(p, s, q, w, a);
  }

  public Map<String, Object> one(String id) {
    return operationDAO.one(id);
  }

  public Map<String, Object> save(String id, Map<String, Object> b, boolean u) {
    return operationDAO.save(id, b, u);
  }

  public Map<String, Object> delete(String id) {
    return operationDAO.delete(id);
  }

  public List<Map<String, Object>> query(String q, Object... a) {
    return operationDAO.query(q, a);
  }
}
