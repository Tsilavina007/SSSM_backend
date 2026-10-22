package sssm.dev.sssm_backend.a11_inventory.qr_scan.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import sssm.dev.sssm_backend.a11_inventory.qr_scan.dto.QrScanJournalEntry;
import sssm.dev.sssm_backend.a11_inventory.qr_scan.dto.QrScanResolveResponse;

/** Typed persistence boundary for the inventory qr scan module. */
public interface QrScanDAO {
  /**
   * Résout un code QR dans une entreprise. La requête est strictement scoped à {@code companyId} :
   * un code d'une autre entreprise est invisible.
   */
  Optional<QrScanResolveResponse> resolveQrCode(String qrCode, String companyId);

  /** Enregistre une résolution dans le journal des scans. */
  void recordScan(QrScanJournalEntry entry);

  List<Map<String, Object>> list(
      Integer page, Integer size, String search, String where, Object... args);

  Map<String, Object> one(String id);

  Map<String, Object> save(String id, Map<String, Object> body, boolean update);

  Map<String, Object> delete(String id);

  List<Map<String, Object>> query(String sql, Object... args);
}
