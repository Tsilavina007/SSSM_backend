package sssm.dev.sssm_backend.a02_organisation.branch.entity;

import java.sql.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import sssm.dev.sssm_backend.a01_common.enums.SiteStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Branch {
  private String id;
  private String company_id;
  private String agence_id;
  private String name;
  private String code;
  private String address;
  private String city;
  private String phone;
  private String email;
  private SiteStatus status;
  private Date created_at;
  private Date updated_at;
}
