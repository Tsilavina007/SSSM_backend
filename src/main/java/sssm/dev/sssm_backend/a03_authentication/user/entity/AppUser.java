package sssm.dev.sssm_backend.a03_authentication.user.entity;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import sssm.dev.sssm_backend.a01_common.enums.UserStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppUser {
    private String id;
    private String company_id;
    private String username;
    private String email;
    private String password_hash;
    private String first_name;
    private String last_name;
    private String phone;
    private UserStatus status;
    private Timestamp last_login_at;
    private Timestamp created_at;
    private Timestamp updated_at;
}
