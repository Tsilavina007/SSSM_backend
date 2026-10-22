package sssm.dev.sssm_backend.a11_inventory.qr_scan.dto.rest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Entrepôt courant d'un article scanné. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QrScanWarehouseRest {
  private String id;
  private String name;
}
