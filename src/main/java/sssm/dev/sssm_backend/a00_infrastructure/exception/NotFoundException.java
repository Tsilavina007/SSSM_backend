package sssm.dev.sssm_backend.a00_infrastructure.exception;
public class NotFoundException extends RuntimeException {
  public NotFoundException(Exception e) {
    super(e);
  }

  public NotFoundException(String message) {
    super(message);
  }
}
