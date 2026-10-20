package sssm.dev.sssm_backend.a05_clients.client.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClientCreateRequest {
  @NotBlank(message = "L'identifiant de l'entreprise est obligatoire")
  @Size(max = 36, message = "36 caractères maximum")
  private String company_id;

  @Size(max = 36, message = "36 caractères maximum")
  private String agency_id;

  @NotBlank(message = "Le code client est obligatoire")
  @Size(max = 80, message = "80 caractères maximum")
  private String code;

  @NotBlank(message = "Le nom du client est obligatoire")
  @Size(max = 180, message = "180 caractères maximum")
  private String name;

  @Size(max = 255, message = "255 caractères maximum")
  private String legal_name;

  @Size(max = 80, message = "80 caractères maximum")
  private String client_type;

  @Size(max = 100, message = "100 caractères maximum")
  private String registration_number;

  @Size(max = 100, message = "100 caractères maximum")
  private String tax_number;

  @Email(message = "L'adresse email est invalide")
  @Size(max = 180, message = "180 caractères maximum")
  private String email;

  @Size(max = 50, message = "50 caractères maximum")
  private String phone;

  private String address;

  @Size(max = 100, message = "100 caractères maximum")
  private String city;

  @Size(max = 100, message = "100 caractères maximum")
  private String country;

  @Min(value = 0, message = "Le délai de paiement doit être positif ou nul")
  private Integer payment_terms_days;

  private Boolean active;
}
