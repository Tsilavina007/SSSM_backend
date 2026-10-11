package sssm.dev.sssm_backend.a06_sites.security_post.dto;

import java.sql.Time;
import lombok.Data;

import sssm.dev.sssm_backend.a01_common.enums.PostStatus;

@Data
public class SecurityPostCreateRequest {
    private String site_id;
    private String name;
    private String code;
    private String description;
    private Integer required_agents;
    private Time start_time;
    private Time end_time;
    private PostStatus status;
}
