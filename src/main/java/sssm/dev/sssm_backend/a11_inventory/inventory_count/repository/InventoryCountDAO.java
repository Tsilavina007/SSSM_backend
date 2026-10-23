package sssm.dev.sssm_backend.a11_inventory.inventory_count.repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import sssm.dev.sssm_backend.a11_inventory.inventory_count.dto.InventoryCountResponse;
import sssm.dev.sssm_backend.a11_inventory.inventory_count.dto.InventoryCountScanResponse;

/** Typed persistence boundary for the inventory count module. */
public interface InventoryCountDAO {
  /** Lignes paginées d'un comptage, dans l'ordre de scan. */
  List<InventoryCountScanResponse> getResults(String id, int page, int pageSize);

  /** Nombre total de lignes d'un comptage. */
  int countResults(String id);

  /** Toutes les lignes d'un comptage, pour la clôture transactionnelle. */
  List<InventoryCountScanResponse> getAllResults(String id);

  /** Enregistre un scan en lisant l'attendu figé à l'ouverture. */
  InventoryCountScanResponse createScan(String inventoryCountId, Map<String, Object> body);

  /** Crée un comptage IN_PROGRESS et son instantané d'attendus. */
  InventoryCountResponse create(
      String companyId, String warehouseId, String reference, String startedBy, String notes);

  /** Vrai si l'entrepôt existe, appartient à l'entreprise et n'est pas archivé. */
  boolean warehouseBelongsToCompany(String companyId, String warehouseId);

  /** Réserve la clôture ; faux si le comptage n'était plus IN_PROGRESS. */
  boolean complete(String id, Timestamp completedAt, String completedBy, String notes);

  /** Réserve l'annulation ; faux si le comptage n'était plus IN_PROGRESS. */
  boolean cancel(String id);

  List<InventoryCountResponse> list(
      Integer page, Integer size, String search, String where, Object... args);

  Optional<InventoryCountResponse> one(String id);

  Optional<InventoryCountResponse> one(String id, String companyId);
}
