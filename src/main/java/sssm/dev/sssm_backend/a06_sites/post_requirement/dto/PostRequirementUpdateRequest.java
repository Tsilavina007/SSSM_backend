package sssm.dev.sssm_backend.a06_sites.post_requirement.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PostRequirementUpdateRequest {
  @Size(max = 180, message = "180 caractères maximum")
  private String requirement;
}
