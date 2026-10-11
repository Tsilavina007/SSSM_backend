package sssm.dev.sssm_backend.authentication.permission.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PermissionResponse {
    private String id;
    private String code;
    private String name;
    private String description;
}
