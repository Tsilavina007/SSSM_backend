package sssm.dev.sssm_backend.a05_clients.client_contact.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClientContactCreateRequest {
  @NotBlank(message = "L'identifiant du client est obligatoire")
  @Size(max = 36, message = "36 caractères maximum")
  private String client_id;

  @NotBlank(message = "Le prénom du contact est obligatoire")
  @Size(max = 100, message = "100 caractères maximum")
  private String first_name;

  @NotBlank(message = "Le nom du contact est obligatoire")
  @Size(max = 100, message = "100 caractères maximum")
  private String last_name;

  @Size(max = 100, message = "100 caractères maximum")
  private String function;

  @Email(message = "L'adresse email est invalide")
  @Size(max = 180, message = "180 caractères maximum")
  private String email;

  @Size(max = 50, message = "50 caractères maximum")
  private String phone;

  private Boolean is_primary;
}
