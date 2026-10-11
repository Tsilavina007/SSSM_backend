package sssm.dev.sssm_backend.a03_authentication.user.dto;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Data;

import sssm.dev.sssm_backend.a01_common.enums.UserStatus;

@Data
@AllArgsConstructor
public class AppUserResponse {
    private String id;
    private String company_id;
    private String username;
    private String email;
    private String first_name;
    private String last_name;
    private String phone;
    private UserStatus status;
    private Timestamp last_login_at;
    private Timestamp created_at;
    private Timestamp updated_at;
}
