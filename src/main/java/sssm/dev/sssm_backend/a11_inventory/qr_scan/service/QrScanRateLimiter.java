package sssm.dev.sssm_backend.a11_inventory.qr_scan.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import sssm.dev.sssm_backend.a00_infrastructure.exception.TooManyRequestsException;

/**
 * Limitation de débit des résolutions de QR code.
 *
 * <p>Les codes générés ne comportent que huit caractères hexadécimaux : sans limitation, un
 * utilisateur authentifié pourrait énumérer le parc. Le limiteur compte les scans sur une fenêtre
 * glissante, par entreprise et par utilisateur.
 *
 * <p>Le compteur vit en mémoire : il n'est pas partagé entre les instances de l'application et
 * repart à zéro au redémarrage. C'est un garde-fou, pas un mécanisme de facturation.
 */
@Component
public class QrScanRateLimiter {

  private final int maxScans;
  private final long windowMillis;
  private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

  public QrScanRateLimiter(
      @Value("${app.qr.scan.max-per-minute:120}") int maxScans,
      @Value("${app.qr.scan.window-seconds:60}") long windowSeconds) {
    this.maxScans = maxScans;
    this.windowMillis = Duration.ofSeconds(windowSeconds).toMillis();
  }

  /**
   * Vérifie qu'un scan est autorisé. Lève une {@link TooManyRequestsException} (HTTP 429) quand la
   * limite est dépassée.
   */
  public void check(String companyId, String userId) {
    if (maxScans <= 0) {
      return;
    }
    String key = companyId + "|" + userId;
    long now = Instant.now().toEpochMilli();
    Deque<Long> timestamps = hits.computeIfAbsent(key, ignored -> new ArrayDeque<>());
    boolean allowed;
    synchronized (timestamps) {
      while (!timestamps.isEmpty() && now - timestamps.peekFirst() >= windowMillis) {
        timestamps.pollFirst();
      }
      allowed = timestamps.size() < maxScans;
      if (allowed) {
        timestamps.addLast(now);
      }
    }
    if (!allowed) {
      throw new TooManyRequestsException(
          "Limite de débit atteinte",
          "Trop de résolutions de QR code ont été demandées par cet utilisateur. Réessayez dans un instant.",
          "RATE_LIMITED");
    }
    prune(now);
  }

  /** Nombre de scans encore disponibles pour une clé. */
  public int remaining(String companyId, String userId) {
    Deque<Long> timestamps = hits.get(companyId + "|" + userId);
    if (timestamps == null) {
      return maxScans;
    }
    synchronized (timestamps) {
      return Math.max(0, maxScans - timestamps.size());
    }
  }

  /** Supprime les clés dont la fenêtre est écoulée, pour borner la mémoire. */
  private void prune(long now) {
    if (hits.size() < 1024) {
      return;
    }
    hits.entrySet().removeIf(entry -> {
      Deque<Long> timestamps = entry.getValue();
      synchronized (timestamps) {
        while (!timestamps.isEmpty() && now - timestamps.peekFirst() >= windowMillis) {
          timestamps.pollFirst();
        }
        return timestamps.isEmpty();
      }
    });
  }
}
