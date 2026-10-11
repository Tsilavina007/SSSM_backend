package sssm.dev.sssm_backend.a07_contracts.service.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class ServiceCreateRequest {
  private String company_id;
  private String code;
  private String name;
  private String description;
  private String unit;
  private BigDecimal unit_price;
  private BigDecimal vat_rate;
  private Boolean active;
}
