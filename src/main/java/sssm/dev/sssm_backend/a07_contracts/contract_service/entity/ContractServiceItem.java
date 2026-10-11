package sssm.dev.sssm_backend.a07_contracts.contract_service.entity;

import java.math.BigDecimal;
import java.sql.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractServiceItem {
  private String id;
  private String contract_id;
  private String service_id;
  private String site_id;
  private BigDecimal quantity;
  private BigDecimal unit_price;
  private BigDecimal vat_rate;
  private Date start_date;
  private Date end_date;
  private String notes;
}
