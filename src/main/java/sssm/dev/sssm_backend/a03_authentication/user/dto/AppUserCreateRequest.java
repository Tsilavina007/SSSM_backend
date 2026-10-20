package sssm.dev.sssm_backend.a03_authentication.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import sssm.dev.sssm_backend.a01_common.enums.UserStatus;

@Data
public class AppUserCreateRequest {
  @Pattern(
      regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
      message = "L'identifiant de l'entreprise doit être un UUID valide")
  private String company_id;

  @NotBlank(message = "Le nom d'utilisateur est obligatoire")
  @Size(max = 100, message = "100 caractères maximum")
  private String username;

  @NotBlank(message = "L'adresse email est obligatoire")
  @Email(message = "L'adresse email est invalide")
  @Size(max = 180, message = "180 caractères maximum")
  private String email;

  @NotBlank(message = "Le mot de passe est obligatoire")
  @Size(min = 8, max = 128, message = "Le mot de passe doit contenir entre 8 et 128 caractères")
  private String password;

  @Size(max = 100, message = "100 caractères maximum")
  private String first_name;

  @Size(max = 100, message = "100 caractères maximum")
  private String last_name;

  @Size(max = 50, message = "50 caractères maximum")
  private String phone;

  private UserStatus status;
}
