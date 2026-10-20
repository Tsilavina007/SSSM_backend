package sssm.dev.sssm_backend.a03_authentication.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

import sssm.dev.sssm_backend.a01_common.enums.UserStatus;

@Data
public class AppUserUpdateRequest {
  @Email(message = "L'adresse email est invalide")
  @Size(max = 180, message = "180 caractères maximum")
  private String email;

  @Size(
      min = 8,
      max = 128,
      message = "Le mot de passe doit contenir entre 8 et 128 caractères")
  private String password;

  @Size(max = 100, message = "100 caractères maximum")
  private String first_name;

  @Size(max = 100, message = "100 caractères maximum")
  private String last_name;

  @Size(max = 50, message = "50 caractères maximum")
  private String phone;

  private UserStatus status;
}
