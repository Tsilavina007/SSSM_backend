package sssm.dev.sssm_backend.authentication.role.dto;

import lombok.Data;

@Data
public class RoleCreateRequest {
    private String company_id;
    private String name;
    private String description;
}
