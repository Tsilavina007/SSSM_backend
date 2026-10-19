package sssm.dev.sssm_backend.a11_inventory.assignment.service;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a11_inventory.assignment.dto.AssignmentResponse;
import sssm.dev.sssm_backend.a11_inventory.assignment.dto.mapper.AssignmentRestMapper;
import sssm.dev.sssm_backend.a11_inventory.assignment.dto.rest.AssignmentRest;
import sssm.dev.sssm_backend.a11_inventory.assignment.repository.AssignmentDAO;

@Service
@RequiredArgsConstructor
public class AssignmentService {
  private final AssignmentDAO assignmentDAO;
  private final AssignmentRestMapper assignmentRestMapper;

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
    return r != null ? assignmentRestMapper.toRest(r) : Map.of("error", "Not found", "id", id);
  }

  public Object save(String id, Map<String, Object> b, boolean u) {
    Object r = assignmentDAO.save(id, b, u);
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
