package sssm.dev.sssm_backend.a07_contracts.contract.dto;

import java.math.BigDecimal;
import java.sql.Date;
import lombok.Data;
import sssm.dev.sssm_backend.a01_common.enums.ContractStatus;

@Data
public class ContractCreateRequest {
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
}
