package sssm.dev.sssm_backend.a11_inventory.qr_scan.dto;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Entrée du journal des scans : qui, quand, sur quel appareil, avec quel résultat. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QrScanJournalEntry {
  private String company_id;
  private String item_id;
  private String qr_code;
  private String scanned_by;
  private String device_id;
  private String result;
  private Timestamp scanned_at;
}
