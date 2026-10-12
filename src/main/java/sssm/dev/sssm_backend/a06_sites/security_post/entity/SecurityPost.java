package sssm.dev.sssm_backend.a06_sites.security_post.entity;

import java.sql.Time;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import sssm.dev.sssm_backend.a01_common.enums.PostStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecurityPost {
  private String id;
  private String site_id;
  private String name;
  private String code;
  private String description;
  private Integer required_agents;
  private Time start_time;
  private Time end_time;
  private PostStatus status;
  private Timestamp created_at;
}
