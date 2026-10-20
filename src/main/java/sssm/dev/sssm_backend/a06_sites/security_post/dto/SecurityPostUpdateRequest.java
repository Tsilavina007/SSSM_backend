package sssm.dev.sssm_backend.a06_sites.security_post.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.sql.Time;
import lombok.Data;

import sssm.dev.sssm_backend.a01_common.enums.PostStatus;

@Data
public class SecurityPostUpdateRequest {
  @Size(max = 36, message = "36 caractères maximum")
  private String site_id;

  @Size(max = 180, message = "180 caractères maximum")
  private String name;

  @Size(max = 80, message = "80 caractères maximum")
  private String code;

  private String description;

  @Min(value = 1, message = "Le nombre d'agents requis doit être au moins 1")
  private Integer required_agents;

  private Time start_time;
  private Time end_time;
  private PostStatus status;
}
