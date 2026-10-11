package sssm.dev.sssm_backend.a06_sites.site.entity;

import java.math.BigDecimal;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import sssm.dev.sssm_backend.a01_common.enums.SiteStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Site {
  private String id;
  private String company_id;
  private String client_id;
  private String branch_id;
  private String name;
  private String code;
  private String address;
  private String city;
  private BigDecimal latitude;
  private BigDecimal longitude;
  private SiteStatus status;
  private String access_instructions;
  private String risk_level;
  private Timestamp created_at;
  private Timestamp updated_at;
}
