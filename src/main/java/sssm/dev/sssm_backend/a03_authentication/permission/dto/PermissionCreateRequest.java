package sssm.dev.sssm_backend.a03_authentication.permission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PermissionCreateRequest {
  @NotBlank(message = "Le code de la permission est obligatoire")
  @Size(max = 150, message = "150 caractères maximum")
  private String code;

  @NotBlank(message = "Le nom de la permission est obligatoire")
  @Size(max = 150, message = "150 caractères maximum")
  private String name;

  private String description;
}
