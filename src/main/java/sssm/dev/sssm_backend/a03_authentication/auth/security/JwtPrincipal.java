package sssm.dev.sssm_backend.a03_authentication.auth.security;

import java.security.Principal;

/** Identité portée par le jeton d'accès, sans dépendre d'un utilisateur chargé en session. */
public record JwtPrincipal(
    String userId, String username, String companyId, boolean platformAdmin)
    implements Principal {

  @Override
  public String getName() {
    return username;
  }
}
