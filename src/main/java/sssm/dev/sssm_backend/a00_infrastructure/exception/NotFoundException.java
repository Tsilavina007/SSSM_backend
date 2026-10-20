package sssm.dev.sssm_backend.a00_infrastructure.exception;

public class NotFoundException extends RuntimeException {
  private final String detail;
  private final String code;

  public NotFoundException(Exception e) {
    super(e);
    this.detail = null;
    this.code = "NOT_FOUND";
  }

  public NotFoundException(String message) {
    this(message, null, "NOT_FOUND");
  }

  public NotFoundException(String message, String detail, String code) {
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
