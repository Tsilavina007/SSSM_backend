package sssm.dev.sssm_backend.a11_inventory.qr_scan.dto.rest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import sssm.dev.sssm_backend.a01_common.enums.ItemStatus;

/**
 * Résultat enrichi d'une résolution de QR code.
 *
 * <p>La réponse ne contient que des informations exploitables sur le terrain : article, type,
 * catégorie, entrepôt courant et, le cas échéant, l'affectation active avec son détenteur.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QrScanRest {
  private String id;
  private String qr_code;
  private String description;
  private ItemStatus status;
  private QrScanItemTypeRest item_type;
  private QrScanCategoryRest category;
  private QrScanWarehouseRest warehouse;
  private QrScanAssignmentRest active_assignment;
}
