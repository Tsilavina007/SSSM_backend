package sssm.dev.sssm_backend.organisation.agency.dto;

import lombok.Data;

@Data
public class AgencyUpdateRequest {
    private String company_id;
    private String name;
    private String code;
    private String address;
    private String city;
    private String phone;
    private String email;
    private String tax_number;
    private String registration_number;
    private Boolean active;
}
