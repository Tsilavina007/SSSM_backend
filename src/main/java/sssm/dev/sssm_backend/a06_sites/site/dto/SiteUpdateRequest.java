package sssm.dev.sssm_backend.a06_sites.site.dto;

import java.math.BigDecimal;
import lombok.Data;

import sssm.dev.sssm_backend.a01_common.enums.SiteStatus;

@Data
public class SiteUpdateRequest {
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
}
