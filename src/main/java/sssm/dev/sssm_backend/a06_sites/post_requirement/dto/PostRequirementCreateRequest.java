package sssm.dev.sssm_backend.a06_sites.post_requirement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PostRequirementCreateRequest {
  @NotBlank(message = "L'identifiant du poste est obligatoire")
  @Size(max = 36, message = "36 caractères maximum")
  private String post_id;

  @NotBlank(message = "L'exigence est obligatoire")
  @Size(max = 180, message = "180 caractères maximum")
  private String requirement;
}
