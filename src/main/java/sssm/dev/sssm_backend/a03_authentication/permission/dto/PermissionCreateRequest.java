package sssm.dev.sssm_backend.a03_authentication.permission.dto;

import lombok.Data;

@Data
public class PermissionCreateRequest {
    private String code;
    private String name;
    private String description;
}
