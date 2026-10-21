package sssm.dev.sssm_backend.a11_inventory.assignment.service;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sssm.dev.sssm_backend.a01_common.enums.ItemStatus;
import sssm.dev.sssm_backend.a01_common.errors.ItemError;
import sssm.dev.sssm_backend.a11_inventory.assignment.dto.AssignmentResponse;
import sssm.dev.sssm_backend.a11_inventory.assignment.dto.mapper.AssignmentRestMapper;
import sssm.dev.sssm_backend.a11_inventory.assignment.dto.rest.AssignmentRest;
import sssm.dev.sssm_backend.a11_inventory.assignment.repository.AssignmentDAO;
import sssm.dev.sssm_backend.a11_inventory.item.repository.ItemDAO;

@Service
@RequiredArgsConstructor
public class AssignmentService {
  private final AssignmentDAO assignmentDAO;
  private final AssignmentRestMapper assignmentRestMapper;
  private final ItemDAO itemDAO;

  public List<AssignmentRest> listActive(Integer page, Integer size) {
    return mapperListResponseToRest(assignmentDAO.listActive(page, size));
  }

  public List<AssignmentRest> listByEmployee(String id) {
    return mapperListResponseToRest(assignmentDAO.listByEmployee(id));
  }

  public List<AssignmentRest> listByPost(String id) {
    return mapperListResponseToRest(assignmentDAO.listByPost(id));
  }

  public List<AssignmentRest> list(Integer p, Integer s, String q, String w, Object... a) {
    return mapperListResponseToRest(assignmentDAO.list(p, s, q, w, a));
  }

  public Object one(String id) {
    AssignmentResponse r = assignmentDAO.one(id).orElse(null);
    return r != null
        ? assignmentRestMapper.toRest(r)
        : Map.of(
            "errors", new ItemError("Affectation d'article introuvable", "Aucune affectation d'article ne correspond à l'identifiant " + id + ".", "NOT_FOUND"));
  }

  @Transactional
  public Object save(String id, Map<String, Object> b, boolean u) {
    Object r = assignmentDAO.save(id, b, u);
    if (r instanceof AssignmentResponse rs && rs.getItem_id() != null) {
      if (!u) {
        // Nouvelle affectation : l'article quitte le stock disponible.
        itemDAO.updateItemStatus(rs.getItem_id(), ItemStatus.ASSIGNED);
        itemDAO.updateItemLocation(rs.getItem_id(), null);
      } else if (rs.getReturned_at() != null) {
        // Restitution : l'article redevient disponible.
        itemDAO.updateItemStatus(rs.getItem_id(), ItemStatus.AVAILABLE);
      }
    }
    return r instanceof AssignmentResponse rs ? assignmentRestMapper.toRest(rs) : r;
  }

  public Map<String, Object> delete(String id) {
    return assignmentDAO.delete(id);
  }

  public List<Map<String, Object>> query(String q, Object... a) {
    return assignmentDAO.query(q, a);
  }

  private List<AssignmentRest> mapperListResponseToRest(List<AssignmentResponse> items) {
    return items.stream()
        .map(
            item -> {
              return assignmentRestMapper.toRest(item);
            })
        .toList();
  }
}
