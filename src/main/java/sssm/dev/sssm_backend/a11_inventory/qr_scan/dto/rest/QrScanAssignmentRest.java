package sssm.dev.sssm_backend.a11_inventory.qr_scan.dto.rest;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Affectation active d'un article scanné, avec son détenteur et son poste. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QrScanAssignmentRest {
  private String id;
  private Timestamp assigned_at;
  private Timestamp expected_return_at;
  private QrScanEmployeeRest employee;
  private QrScanPostRest post;
}
