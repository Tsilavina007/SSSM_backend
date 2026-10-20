package sssm.dev.sssm_backend.a06_sites.post_requirement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostRequirementResponse {
  private String id;
  private String post_id;
  private String requirement;
}
