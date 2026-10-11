package sssm.dev.sssm_backend.a06_sites.post_requirement.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostRequirement {
  private String id;
  private String post_id;
  private String requirement;
}
