package sssm.dev.sssm_backend.a11_inventory.dashboard.service;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a11_inventory.dashboard.repository.DashboardDAO;

@Service
@RequiredArgsConstructor
public class DashboardService {
  private final DashboardDAO dashboardDAO;

  public List<Map<String, Object>> getSummary() {
    return dashboardDAO.getSummary();
  }

  public List<Map<String, Object>> getLowStock() {
    return dashboardDAO.getLowStock();
  }

  public List<Map<String, Object>> list(Integer p, Integer s, String q, String w, Object... a) {
    return dashboardDAO.list(p, s, q, w, a);
  }

  public Map<String, Object> one(String id) {
    return dashboardDAO.one(id);
  }

  public Map<String, Object> save(String id, Map<String, Object> b, boolean u) {
    return dashboardDAO.save(id, b, u);
  }

  public Map<String, Object> delete(String id) {
    return dashboardDAO.delete(id);
  }

  public List<Map<String, Object>> query(String q, Object... a) {
    return dashboardDAO.query(q, a);
  }
}
