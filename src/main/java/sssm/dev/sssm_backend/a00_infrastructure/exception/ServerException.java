package sssm.dev.sssm_backend.a00_infrastructure.exception;
public class ServerException extends RuntimeException {
  private final String detail;
  private final String code;

  public ServerException(Exception e) {
    super(e);
    this.detail = null;
    this.code = "SERVER_ERROR";
  }

  public ServerException(String message) {
    this(message, null, "SERVER_ERROR");
  }

  public ServerException(String message, String detail, String code) {
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
