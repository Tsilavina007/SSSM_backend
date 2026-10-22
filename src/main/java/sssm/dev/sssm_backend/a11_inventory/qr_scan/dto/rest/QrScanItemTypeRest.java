package sssm.dev.sssm_backend.a11_inventory.qr_scan.dto.rest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Type d'article d'un article scanné. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QrScanItemTypeRest {
  private String id;
  private String code;
  private String name;
}
