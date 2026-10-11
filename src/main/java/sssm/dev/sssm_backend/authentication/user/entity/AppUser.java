package sssm.dev.sssm_backend.authentication.user.entity;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String status;
    private Timestamp last_login_at;
    private Timestamp created_at;
    private Timestamp updated_at;
}
