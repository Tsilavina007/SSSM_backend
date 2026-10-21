package sssm.dev.sssm_backend.a00_infrastructure.exception;

/** Exception métier pour un jeton d'authentification absent, invalide ou révoqué. */
public class UnauthorizedException extends RuntimeException {
  private final String detail;

  public UnauthorizedException(String message, String detail) {
    super(message);
    this.detail = detail;
  }

  public String detail() {
    return detail;
  }
}
