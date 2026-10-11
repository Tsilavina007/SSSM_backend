package sssm.dev.sssm_backend.a07_contracts.contract.entity;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import sssm.dev.sssm_backend.a01_common.enums.ContractStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Contract {
  private String id;
  private String company_id;
  private String client_id;
  private String contract_number;
  private String title;
  private Date start_date;
  private Date end_date;
  private ContractStatus status;
  private String billing_frequency;
  private String currency_code;
  private Integer payment_terms_days;
  private BigDecimal total_amount;
  private String notes;
  private Timestamp created_at;
  private Timestamp updated_at;
}
