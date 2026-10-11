package sssm.dev.sssm_backend.a02_organisation.company.dto;

import lombok.Data;

@Data
public class CompanyUpdateRequest {
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
}
