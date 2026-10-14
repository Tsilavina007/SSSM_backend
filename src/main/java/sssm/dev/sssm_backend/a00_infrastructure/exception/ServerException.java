package sssm.dev.sssm_backend.a00_infrastructure.exception;
public class ServerException extends RuntimeException {
  public ServerException(Exception e) {
    super(e);
  }

  public ServerException(String message) {
    super(message);
  }
}
