package sssm.dev.sssm_backend.authentication.user.dto;

import lombok.Data;

@Data
public class AppUserUpdateRequest {
    private String email;
    private String password;
    private String first_name;
    private String last_name;
    private String phone;
    private String status;
}
