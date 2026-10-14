package sssm.dev.sssm_backend.a00_infrastructure.exception;
public class ClientException extends RuntimeException {
  public ClientException(Exception e) {
    super(e);
  }

  public ClientException(String message) {
    super(message);
  }
}
