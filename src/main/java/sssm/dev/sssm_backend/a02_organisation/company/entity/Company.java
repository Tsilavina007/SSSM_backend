package sssm.dev.sssm_backend.a02_organisation.company.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;

import lombok.AllArgsConstructor;

// Company Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Company {
    private String id;
    private String name;
    private String legal_name;
    private String registration_number;
    private String tax_number;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String country;
    private String logo_url;
    private String currency_code;
    private Date created_at;
    private Date updated_at;
}
