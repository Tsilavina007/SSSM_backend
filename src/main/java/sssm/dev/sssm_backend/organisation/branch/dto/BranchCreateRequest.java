package sssm.dev.sssm_backend.organisation.branch.dto;

import lombok.Data;

@Data
public class BranchCreateRequest {
    private String company_id;
    private String agence_id;
    private String name;
    private String code;
    private String address;
    private String city;
    private String phone;
    private String email;
    private String status;
}
