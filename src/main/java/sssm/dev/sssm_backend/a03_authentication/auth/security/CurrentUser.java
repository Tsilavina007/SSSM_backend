package sssm.dev.sssm_backend.a03_authentication.auth.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Accès contrôlé à l'identité JWT courante dans les services métier. */
@Component
public class CurrentUser {

  public JwtPrincipal require() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
      throw new AccessDeniedException("Authentification JWT requise.");
    }
    return principal;
  }

  public boolean isPlatformAdmin() {
    return require().platformAdmin();
  }

  public String companyId() {
    return require().companyId();
  }

  public String requireCompanyId() {
    String companyId = companyId();
    if (companyId == null || companyId.isBlank()) {
      throw new AccessDeniedException("Le compte n'est rattaché à aucune entreprise.");
    }
    return companyId;
  }
}
