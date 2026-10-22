package sssm.dev.sssm_backend.a11_inventory.qr_scan.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a03_authentication.auth.security.CurrentUser;
import sssm.dev.sssm_backend.a11_inventory.qr_scan.dto.QrScanJournalEntry;
import sssm.dev.sssm_backend.a11_inventory.qr_scan.dto.QrScanResolveResponse;
import sssm.dev.sssm_backend.a11_inventory.qr_scan.dto.mapper.QrScanRestMapper;
import sssm.dev.sssm_backend.a11_inventory.qr_scan.dto.rest.QrScanRest;
import sssm.dev.sssm_backend.a11_inventory.qr_scan.repository.QrScanDAO;

/**
 * Résolution des QR codes.
 *
 * <p>La résolution est strictement scoped à l'entreprise de l'appelant : un code existant dans une
 * autre entreprise est traité comme inconnu. Chaque tentative, résolue ou non, alimente le journal
 * des scans.
 */
@Service
@RequiredArgsConstructor
public class QrScanService {
  private final QrScanDAO qrScanDAO;
  private final QrScanRestMapper qrScanRestMapper;
  private final CurrentUser currentUser;
  private final QrScanRateLimiter rateLimiter;

  /**
   * Résout un code QR.
   *
   * <p>La méthode n'est pas transactionnelle : une résolution qui se termine par un 404 doit
   * laisser le journal intact, alors qu'un rollback annulerait l'écriture du journal.
   */
  public QrScanRest resolveQrCode(String qrCode, String deviceId) {
    String companyId = currentUser.requireCompanyId();
    String userId = currentUser.require().userId();
    rateLimiter.check(companyId, userId);

    Optional<QrScanResolveResponse> found = qrScanDAO.resolveQrCode(qrCode, companyId);
    if (found.isEmpty()) {
      qrScanDAO.recordScan(
          new QrScanJournalEntry(
              companyId,
              null,
              qrCode,
              userId,
              deviceId,
              "NOT_FOUND",
              Timestamp.from(Instant.now())));
      throw new NotFoundException(
          "QR code introuvable",
          "Aucun article de l'entreprise ne porte le code « " + qrCode + " ».",
          "NOT_FOUND");
    }

    QrScanResolveResponse resolved = found.get();
    qrScanDAO.recordScan(
        new QrScanJournalEntry(
            companyId,
            resolved.getId(),
            qrCode,
            userId,
            deviceId,
            "RESOLVED",
            Timestamp.from(Instant.now())));
    return qrScanRestMapper.toRest(resolved);
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
}
