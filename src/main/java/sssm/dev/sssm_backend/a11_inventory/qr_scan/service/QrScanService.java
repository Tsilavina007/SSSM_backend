package sssm.dev.sssm_backend.a11_inventory.qr_scan.service;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a11_inventory.qr_scan.dto.QrScanItemResponse;
import sssm.dev.sssm_backend.a11_inventory.qr_scan.dto.mapper.QrScanItemRestMapper;
import sssm.dev.sssm_backend.a11_inventory.qr_scan.dto.rest.QrScanItemRest;
import sssm.dev.sssm_backend.a11_inventory.qr_scan.repository.QrScanDAO;

@Service
@RequiredArgsConstructor
public class QrScanService {
  private final QrScanDAO qrScanDAO;
  private final QrScanItemRestMapper qrScanItemRestMapper;

  public List<QrScanItemRest> resolveQrCode(String qrCode) {
    return mapperListResponseToRest(qrScanDAO.resolveQrCode(qrCode));
  }

  public List<Map<String, Object>> list(Integer p, Integer s, String q, String w, Object... a) {
    return qrScanDAO.list(p, s, q, w, a);
  }

  public Map<String, Object> one(String id) {
    return qrScanDAO.one(id);
  }

  public Map<String, Object> save(String id, Map<String, Object> b, boolean u) {
    return qrScanDAO.save(id, b, u);
  }

  public Map<String, Object> delete(String id) {
    return qrScanDAO.delete(id);
  }

  public List<Map<String, Object>> query(String q, Object... a) {
    return qrScanDAO.query(q, a);
  }

  private List<QrScanItemRest> mapperListResponseToRest(List<QrScanItemResponse> items) {
    return items.stream()
        .map(
            item -> {
              return qrScanItemRestMapper.toRest(item);
            })
        .toList();
  }
}
