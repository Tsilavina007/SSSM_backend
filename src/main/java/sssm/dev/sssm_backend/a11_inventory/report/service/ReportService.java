package sssm.dev.sssm_backend.a11_inventory.report.service;

import java.util.*;

import sssm.dev.sssm_backend.a11_inventory.report.repository.ReportDAO;
import sssm.dev.sssm_backend.a11_inventory.report.repository.ReportDAOImpl;

public class ReportService {
  private final ReportDAO dao;

  public ReportService() {
    this.dao = new ReportDAOImpl();
  }

  public List<Map<String, Object>> list(Integer p, Integer s, String q, String w, Object... a) {
    return dao.list(p, s, q, w, a);
  }

  public Map<String, Object> one(String id) {
    return dao.one(id);
  }

  public Map<String, Object> save(String id, Map<String, Object> b, boolean u) {
    return dao.save(id, b, u);
  }

  public Map<String, Object> delete(String id) {
    return dao.delete(id);
  }

  public List<Map<String, Object>> query(String q, Object... a) {
    return dao.query(q, a);
  }
}

