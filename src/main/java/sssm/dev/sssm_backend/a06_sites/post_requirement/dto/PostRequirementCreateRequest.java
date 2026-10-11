package sssm.dev.sssm_backend.a06_sites.post_requirement.dto;

import lombok.Data;

@Data
public class PostRequirementCreateRequest {
  private String post_id;
  private String requirement;
}
