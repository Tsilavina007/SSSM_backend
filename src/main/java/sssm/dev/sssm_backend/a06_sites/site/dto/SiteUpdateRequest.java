package sssm.dev.sssm_backend.a06_sites.site.dto;

import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

import sssm.dev.sssm_backend.a01_common.enums.SiteStatus;

@Data
public class SiteUpdateRequest {
  @Size(max = 36, message = "36 caractères maximum")
  private String company_id;

  @Size(max = 36, message = "36 caractères maximum")
  private String client_id;

  @Size(max = 36, message = "36 caractères maximum")
  private String branch_id;

  @Size(max = 180, message = "180 caractères maximum")
  private String name;

  @Size(max = 80, message = "80 caractères maximum")
  private String code;

  private String address;

  @Size(max = 100, message = "100 caractères maximum")
  private String city;

  private BigDecimal latitude;
  private BigDecimal longitude;
  private SiteStatus status;
  private String access_instructions;

  @Size(max = 30, message = "30 caractères maximum")
  private String risk_level;
}
