package sssm.dev.sssm_backend.a05_clients.client_contact.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClientContactUpdateRequest {
  @Size(max = 100, message = "100 caractères maximum")
  private String first_name;

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
