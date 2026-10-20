package sssm.dev.sssm_backend.a03_authentication.role.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RoleUpdateRequest {
  @Size(max = 36, message = "36 caractères maximum")
  private String company_id;

  @Size(max = 100, message = "100 caractères maximum")
  private String name;

  private String description;
}
