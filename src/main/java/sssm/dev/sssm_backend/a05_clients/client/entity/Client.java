package sssm.dev.sssm_backend.a05_clients.client.entity;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Client {
    private String id;
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
    private Timestamp created_at;
    private Timestamp updated_at;
}
