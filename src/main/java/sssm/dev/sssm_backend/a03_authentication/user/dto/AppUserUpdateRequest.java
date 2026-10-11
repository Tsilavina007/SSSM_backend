package sssm.dev.sssm_backend.a03_authentication.user.dto;

import lombok.Data;

import sssm.dev.sssm_backend.a01_common.enums.UserStatus;

@Data
public class AppUserUpdateRequest {
    private String email;
    private String password;
    private String first_name;
    private String last_name;
    private String phone;
    private UserStatus status;
}
