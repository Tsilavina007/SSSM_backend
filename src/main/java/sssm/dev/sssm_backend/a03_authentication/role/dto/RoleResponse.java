package sssm.dev.sssm_backend.a03_authentication.role.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RoleResponse {
    private String id;
    private String company_id;
    private String name;
    private String description;
}
