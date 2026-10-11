package sssm.dev.sssm_backend.authentication.permission.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Permission {
    private String id;
    private String code;
    private String name;
    private String description;
}
