package sssm.dev.sssm_backend.a00_infrastructure.exception;

public class ForbiddenException extends RuntimeException {
  private final String detail;

  public ForbiddenException(String message, String detail) {
    super(message);
    this.detail = detail;
  }

  public String detail() {
    return detail;
  }
}