package sssm.dev.sssm_backend.a00_infrastructure.exception;
/** Depassement de la limite de debit, traduit en HTTP 429 par le gestionnaire global. */
public class TooManyRequestsException extends RuntimeException {
  private final String detail;
  private final String code;

  public TooManyRequestsException(String message) {
    this(message, null, "RATE_LIMITED");
  }

  public TooManyRequestsException(String message, String detail, String code) {
    super(message);
    this.detail = detail;
    this.code = code;
  }

  public String detail() {
    return detail;
  }

  public String code() {
    return code;
  }
}
