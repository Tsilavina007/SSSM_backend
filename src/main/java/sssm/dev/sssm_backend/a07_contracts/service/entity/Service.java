package sssm.dev.sssm_backend.a07_contracts.service.entity;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Service {
  private String id;
  private String company_id;
  private String code;
  private String name;
  private String description;
  private String unit;
  private BigDecimal unit_price;
  private BigDecimal vat_rate;
  private Boolean active;
}
