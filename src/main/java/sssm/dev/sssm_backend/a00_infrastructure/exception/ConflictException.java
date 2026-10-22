package sssm.dev.sssm_backend.a00_infrastructure.exception;

/** Conflit métier ou d'état, traduit en HTTP 409 par le gestionnaire global. */
public class ConflictException extends RuntimeException {
  private final String detail;
  private final String code;

  public ConflictException(String message) {
    this(message, null, "CONFLICT");
  }

  public ConflictException(String message, String detail, String code) {
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
