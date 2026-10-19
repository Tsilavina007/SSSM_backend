package sssm.dev.sssm_backend.a11_inventory.report.service;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a11_inventory.report.repository.ReportDAO;

@Service
@RequiredArgsConstructor
public class ReportService {
  private final ReportDAO reportDAO;

  public List<Map<String, Object>> list(Integer p, Integer s, String q, String w, Object... a) {
    return reportDAO.list(p, s, q, w, a);
  }

  public Map<String, Object> one(String id) {
    return reportDAO.one(id);
  }

  public Map<String, Object> save(String id, Map<String, Object> b, boolean u) {
    return reportDAO.save(id, b, u);
  }

  public Map<String, Object> delete(String id) {
    return reportDAO.delete(id);
  }

  public List<Map<String, Object>> query(String q, Object... a) {
    return reportDAO.query(q, a);
  }
}
