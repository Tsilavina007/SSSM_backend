package sssm.dev.sssm_backend.a02_organisation.agency.entity;

import java.sql.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Agency {
  private String id;
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
  private Date created_at;
  private Date updated_at;
}
