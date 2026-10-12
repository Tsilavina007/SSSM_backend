package sssm.dev.sssm_backend.a11_inventory.qr_scan.repository;

import java.util.List;
import java.util.Map;

/** Typed persistence boundary for the inventory qr scan module. */
public interface QrScanDAO {
  List<Map<String, Object>> list(
      Integer page, Integer size, String search, String where, Object... args);

  Map<String, Object> one(String id);

  Map<String, Object> save(String id, Map<String, Object> body, boolean update);

  Map<String, Object> delete(String id);

  List<Map<String, Object>> query(String sql, Object... args);
}
