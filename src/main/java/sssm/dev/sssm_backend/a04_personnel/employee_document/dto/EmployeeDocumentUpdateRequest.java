package sssm.dev.sssm_backend.a04_personnel.employee_document.dto;

import jakarta.validation.constraints.Size;
import java.sql.Date;
import lombok.Data;

@Data
public class EmployeeDocumentUpdateRequest {
  @Size(max = 36, message = "36 caractères maximum")
  private String employee_id;

  @Size(max = 100, message = "100 caractères maximum")
  private String document_type;

  @Size(max = 150, message = "150 caractères maximum")
  private String document_number;

  private Date issue_date;
  private Date expiry_date;
  private String file_url;
  private String notes;
}
