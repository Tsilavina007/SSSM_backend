package sssm.dev.sssm_backend.a11_inventory.operation.service;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sssm.dev.sssm_backend.a01_common.errors.ItemError;
import sssm.dev.sssm_backend.a11_inventory.operation.dto.mapper.OperationRestMapper;
import sssm.dev.sssm_backend.a11_inventory.operation.dto.OperationResponse;
import sssm.dev.sssm_backend.a11_inventory.operation.repository.OperationDAO;

@Service
@RequiredArgsConstructor
public class OperationService {
  private final OperationDAO operationDAO;
  private final OperationRestMapper operationRestMapper;

  public List<Map<String, Object>> list(Integer p, Integer s, String q, String w, Object... a) {
    return operationDAO.list(p, s, q, w, a);
  }

  public Object one(String id) {
    OperationResponse r = operationDAO.one(id).orElse(null);
    return r != null
        ? operationRestMapper.toRest(r)
        : Map.of(
            "errors", new ItemError("Opération d'inventaire introuvable", "Aucune opération d'inventaire ne correspond à l'identifiant " + id + ".", "NOT_FOUND"));
  }

  @Transactional
  public Object save(String id, Map<String, Object> b, boolean u) {
    Object r = operationDAO.save(id, b, u);
    return r instanceof OperationResponse rs ? operationRestMapper.toRest(rs) : r;
  }

  public Map<String, Object> delete(String id) {
    return operationDAO.delete(id);
  }

  public List<Map<String, Object>> query(String q, Object... a) {
    return operationDAO.query(q, a);
  }
}
