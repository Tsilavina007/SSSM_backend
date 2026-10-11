package sssm.dev.sssm_backend.a05_clients.client.dto;

import lombok.Data;

@Data
public class ClientCreateRequest {
    private String company_id;
    private String agency_id;
    private String code;
    private String name;
    private String legal_name;
    private String client_type;
    private String registration_number;
    private String tax_number;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String country;
    private Integer payment_terms_days;
    private Boolean active;
}
