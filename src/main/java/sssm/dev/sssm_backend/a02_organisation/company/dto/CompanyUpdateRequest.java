package sssm.dev.sssm_backend.a02_organisation.company.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CompanyUpdateRequest {
  @Size(max = 180, message = "180 caractères maximum")
  private String name;

  @Size(max = 255, message = "255 caractères maximum")
  private String legal_name;

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

  private String logo_url;

  @Size(min = 3, max = 3, message = "Le code devise doit contenir 3 caractères")
  private String currency_code;
}
