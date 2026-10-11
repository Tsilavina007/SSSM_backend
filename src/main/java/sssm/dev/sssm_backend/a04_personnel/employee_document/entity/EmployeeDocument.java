package sssm.dev.sssm_backend.a04_personnel.employee_document.entity;

import java.sql.Date;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDocument {
  private String id;
  private String employee_id;
  private String document_type;
  private String document_number;
  private Date issue_date;
  private Date expiry_date;
  private String file_url;
  private String notes;
  private Timestamp created_at;
}
