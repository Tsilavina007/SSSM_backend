package sssm.dev.sssm_backend.a03_authentication.permission.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PermissionUpdateRequest {
  @Size(max = 150, message = "150 caractères maximum")
  private String code;

  @Size(max = 150, message = "150 caractères maximum")
  private String name;

  private String description;
}
