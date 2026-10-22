package sssm.dev.sssm_backend.a11_inventory.qr_scan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import sssm.dev.sssm_backend.a00_infrastructure.exception.TooManyRequestsException;
import sssm.dev.sssm_backend.a11_inventory.qr_scan.service.QrScanRateLimiter;

/** Limitation de débit des résolutions de QR code. */
class QrScanRateLimiterTests {

  @Test
  void scansAreAllowedUpToTheLimit() {
    QrScanRateLimiter limiter = new QrScanRateLimiter(3, 60);

    limiter.check("company-1", "user-1");
    limiter.check("company-1", "user-1");
    limiter.check("company-1", "user-1");

    assertEquals(0, limiter.remaining("company-1", "user-1"));
  }

  @Test
  void scanBeyondTheLimitIsRejected() {
    QrScanRateLimiter limiter = new QrScanRateLimiter(2, 60);
    limiter.check("company-1", "user-1");
    limiter.check("company-1", "user-1");

    TooManyRequestsException exception =
        assertThrows(
            TooManyRequestsException.class, () -> limiter.check("company-1", "user-1"));
    assertEquals("RATE_LIMITED", exception.code());
  }

  @Test
  void limitIsAppliedPerUser() {
    QrScanRateLimiter limiter = new QrScanRateLimiter(1, 60);
    limiter.check("company-1", "user-1");

    // Un autre utilisateur, ou une autre entreprise, n'est pas pénalisé.
    limiter.check("company-1", "user-2");
    limiter.check("company-2", "user-1");
  }

  @Test
  void limiterCanBeDisabled() {
    QrScanRateLimiter limiter = new QrScanRateLimiter(0, 60);

    for (int index = 0; index < 50; index++) {
      limiter.check("company-1", "user-1");
    }
  }
}
