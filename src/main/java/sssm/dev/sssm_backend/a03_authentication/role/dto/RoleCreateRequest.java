package sssm.dev.sssm_backend.a03_authentication.role.dto;

import lombok.Data;

@Data
public class RoleCreateRequest {
    private String company_id;
    private String name;
    private String description;
}
