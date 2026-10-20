package sssm.dev.sssm_backend.a02_organisation.branch.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

import sssm.dev.sssm_backend.a01_common.enums.SiteStatus;

@Data
public class BranchUpdateRequest {
  @Size(max = 36, message = "36 caractères maximum")
  private String company_id;

  @Size(max = 36, message = "36 caractères maximum")
  private String agence_id;

  @Size(max = 150, message = "150 caractères maximum")
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

  private SiteStatus status;
}
