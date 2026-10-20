package sssm.dev.sssm_backend.a00_infrastructure.exception;

public class ClientException extends RuntimeException {
  private final String detail;
  private final String code;

  public ClientException(Exception e) {
    super(e);
    this.detail = null;
    this.code = "CLIENT_ERROR";
  }

  public ClientException(String message) {
    this(message, null, "CLIENT_ERROR");
  }

  public ClientException(String message, String detail, String code) {
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
