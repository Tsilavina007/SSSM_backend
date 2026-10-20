package sssm.dev.sssm_backend.a02_organisation.agency.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AgencyCreateRequest {
  @NotBlank(message = "L'identifiant de l'entreprise est obligatoire")
  @Size(max = 36, message = "36 caractères maximum")
  private String company_id;

  @NotBlank(message = "Le nom de l'agence est obligatoire")
  @Size(max = 180, message = "180 caractères maximum")
  private String name;

  @Size(max = 50, message = "50 caractères maximum")
  private String code;

  private String address;

  @Size(max = 100, message = "100 caractères maximum")
  private String city;

  @Size(max = 50, message = "50 caractères maximum")
  private String phone;

  @Email(message = "L'adresse email est invalide")
  @Size(max = 180, message = "180 caractères maximum")
  private String email;

  @Size(max = 100, message = "100 caractères maximum")
  private String tax_number;

  @Size(max = 100, message = "100 caractères maximum")
  private String registration_number;

  private Boolean active;
}
