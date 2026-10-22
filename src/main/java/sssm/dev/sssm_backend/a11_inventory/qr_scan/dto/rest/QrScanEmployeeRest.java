package sssm.dev.sssm_backend.a11_inventory.qr_scan.dto.rest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Employé détenteur d'un article scanné. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QrScanEmployeeRest {
  private String id;
  private String employee_number;
  private String first_name;
  private String last_name;
}
