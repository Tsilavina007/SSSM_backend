package sssm.dev.sssm_backend.a11_inventory.report.service;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a01_common.pagination.PageQuery;
import sssm.dev.sssm_backend.a01_common.pagination.PageResponse;
import sssm.dev.sssm_backend.a11_inventory.report.dto.ReportMovementResponse;
import sssm.dev.sssm_backend.a11_inventory.report.dto.mapper.ReportMovementRestMapper;
import sssm.dev.sssm_backend.a11_inventory.report.dto.rest.ReportMovementRest;
import sssm.dev.sssm_backend.a11_inventory.report.repository.ReportDAO;

@Service
@RequiredArgsConstructor
public class ReportService {
  private final ReportDAO reportDAO;
  private final ReportMovementRestMapper reportMovementRestMapper;

  public PageResponse<ReportMovementRest> list(
      Integer p, Integer s, String q, String w, Object... a) {
    PageQuery query = PageQuery.of(p, s, 50);
    List<ReportMovementRest> movements =
        mapperListResponseToRest(reportDAO.list(query.page(), query.pageSize(), q, w, a));
    return PageResponse.of(movements, query, reportDAO.countList(q, w, a));
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

  private List<ReportMovementRest> mapperListResponseToRest(List<ReportMovementResponse> items) {
    return items.stream()
        .map(
            item -> {
              return reportMovementRestMapper.toRest(item);
            })
        .toList();
  }
}
