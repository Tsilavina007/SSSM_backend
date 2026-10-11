package sssm.dev.sssm_backend.organisation.branch.dto;

import java.sql.Date;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BranchResponse {
    private String id;
    private String company_id;
    private String agence_id;
    private String name;
    private String code;
    private String address;
    private String city;
    private String phone;
    private String email;
    private String status;
    private Date created_at;
    private Date updated_at;
}
