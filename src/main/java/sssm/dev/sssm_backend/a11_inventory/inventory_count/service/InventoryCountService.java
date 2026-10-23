package sssm.dev.sssm_backend.a11_inventory.inventory_count.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sssm.dev.sssm_backend.a00_infrastructure.exception.ClientException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ConflictException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a01_common.enums.ItemStatus;
import sssm.dev.sssm_backend.a01_common.enums.MovementType;
import sssm.dev.sssm_backend.a03_authentication.auth.security.CurrentUser;
import sssm.dev.sssm_backend.a03_authentication.auth.security.JwtPrincipal;
import sssm.dev.sssm_backend.a11_inventory.inventory_count.dto.InventoryCountCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.inventory_count.dto.InventoryCountResponse;
import sssm.dev.sssm_backend.a11_inventory.inventory_count.dto.InventoryCountScanResponse;
import sssm.dev.sssm_backend.a11_inventory.inventory_count.dto.mapper.InventoryCountRestMapper;
import sssm.dev.sssm_backend.a11_inventory.inventory_count.dto.mapper.InventoryCountScanRestMapper;
import sssm.dev.sssm_backend.a11_inventory.inventory_count.dto.rest.InventoryCountRest;
import sssm.dev.sssm_backend.a11_inventory.inventory_count.dto.rest.InventoryCountScanRest;
import sssm.dev.sssm_backend.a11_inventory.inventory_count.repository.InventoryCountDAO;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemResponse;
import sssm.dev.sssm_backend.a11_inventory.item.repository.ItemDAO;
import sssm.dev.sssm_backend.a11_inventory.stock_balance.service.StockBalanceService;
import sssm.dev.sssm_backend.a11_inventory.stock_movement.dto.StockMovementCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.stock_movement.repository.StockMovementDAO;

@Service
@RequiredArgsConstructor
public class InventoryCountService {
  private final InventoryCountDAO inventoryCountDAO;
  private final InventoryCountRestMapper inventoryCountRestMapper;
  private final InventoryCountScanRestMapper inventoryCountScanRestMapper;
  private final ItemDAO itemDAO;
  private final StockBalanceService stockBalanceService;
  private final StockMovementDAO stockMovementDAO;
  private final CurrentUser currentUser;

  public List<InventoryCountScanRest> getResults(String id, int page, int pageSize) {
    requireCount(id);
    return mapperListScanResponseToRest(inventoryCountDAO.getResults(id, page, pageSize));
  }

  public int countResults(String id) {
    requireCount(id);
    return inventoryCountDAO.countResults(id);
  }

  /**
   * Ouvre un comptage pour l'entreprise du porteur du jeton et fige l'instantané des attendus.
   *
   * <p>L'entreprise, le statut, l'auteur et l'horodatage de départ sont pilotés par le serveur : le
   * client ne fournit que l'entrepôt, la référence et une note libre.
   */
  @Transactional
  public InventoryCountRest create(InventoryCountCreateRequest request) {
    String companyId = currentUser.requireCompanyId();
    String warehouseId = trimmed(request == null ? null : request.getWarehouse_id());
    if (warehouseId == null) {
      throw new ClientException(
          "Entrepôt obligatoire",
          "Le champ 'warehouse_id' est obligatoire.",
          "CLIENT_ERROR");
    }
    if (!inventoryCountDAO.warehouseBelongsToCompany(companyId, warehouseId)) {
      throw new NotFoundException(
          "Entrepôt introuvable",
          "Aucun entrepôt actif de votre entreprise ne correspond à l'identifiant "
              + warehouseId
              + ".",
          "NOT_FOUND");
    }
    String reference = trimmed(request.getReference());
    if (reference == null) {
      throw new ClientException(
          "Référence obligatoire",
          "Le champ 'reference' est obligatoire.",
          "CLIENT_ERROR");
    }
    String notes = trimmed(request.getNotes());
    InventoryCountResponse created =
        inventoryCountDAO.create(companyId, warehouseId, reference, currentUserId(), notes);
    return inventoryCountRestMapper.toRest(created);
  }

  @Transactional
  public InventoryCountScanRest scan(String id, Map<String, Object> b) {
    InventoryCountResponse count = requireCount(id);
    if (!"IN_PROGRESS".equals(count.getStatus())) {
      throw new ConflictException(
          "Comptage déjà clôturé",
          "Un scan ne peut être ajouté qu'à un comptage IN_PROGRESS.",
          "COUNT_NOT_OPEN");
    }
    return inventoryCountScanRestMapper.toRest(inventoryCountDAO.createScan(id, b));
  }

  /**
   * Clôture un comptage de manière idempotente.
   *
   * <p>Un comptage déjà COMPLETED est renvoyé tel quel, sans nouvelle écriture. Un comptage vide
   * (aucune ligne) est refusé. La réconciliation est transactionnelle : si une écriture échoue, le
   * statut revient à IN_PROGRESS et aucune correction de solde n'est conservée.
   */
  @Transactional
  public InventoryCountRest close(String id, Map<String, Object> b) {
    InventoryCountResponse count = requireCount(id);
    if ("COMPLETED".equals(count.getStatus())) {
      return inventoryCountRestMapper.toRest(count);
    }
    if ("CANCELLED".equals(count.getStatus())) {
      throw new ConflictException(
          "Comptage annulé",
          "Un comptage annulé ne peut pas être clôturé.",
          "COUNT_CANCELLED");
    }

    Timestamp completedAt = Timestamp.from(Instant.now());
    String completedBy = resolveActor(b == null ? null : b.get("completed_by"));
    String notes = b == null || b.get("notes") == null ? null : String.valueOf(b.get("notes"));

    // Revendication atomique : un seul appel concurrent peut passer IN_PROGRESS -> COMPLETED.
    if (!inventoryCountDAO.complete(id, completedAt, completedBy, notes)) {
      InventoryCountResponse current = requireCount(id);
      if ("COMPLETED".equals(current.getStatus())) {
        return inventoryCountRestMapper.toRest(current);
      }
      throw new ConflictException(
          "Comptage déjà clôturé",
          "Seul un comptage IN_PROGRESS peut être clôturé.",
          "COUNT_NOT_OPEN");
    }

    List<InventoryCountScanResponse> scans = inventoryCountDAO.getAllResults(id);
    if (scans.isEmpty()) {
      throw new ConflictException(
          "Comptage vide",
          "Un comptage sans aucune ligne ne peut pas être clôturé comme complet.",
          "COUNT_EMPTY");
    }

    for (InventoryCountScanResponse scan : scans) {
      reconcile(scan, count, completedAt);
      journalAdjustment(scan, count, completedAt, completedBy);
    }

    return inventoryCountRestMapper.toRest(requireCount(id));
  }

  /**
   * Annule un comptage en cours, sans appliquer d'ajustement. L'opération est idempotente ; un
   * comptage déjà clôturé ne peut pas être annulé.
   */
  @Transactional
  public InventoryCountRest cancel(String id) {
    InventoryCountResponse count = requireCount(id);
    if ("CANCELLED".equals(count.getStatus())) {
      return inventoryCountRestMapper.toRest(count);
    }
    if ("COMPLETED".equals(count.getStatus())) {
      throw new ConflictException(
          "Comptage clôturé",
          "Un comptage clôturé ne peut pas être annulé.",
          "COUNT_COMPLETED");
    }
    if (!inventoryCountDAO.cancel(id)) {
      InventoryCountResponse current = requireCount(id);
      if ("CANCELLED".equals(current.getStatus())) {
        return inventoryCountRestMapper.toRest(current);
      }
      throw new ConflictException(
          "Comptage clôturé",
          "Un comptage clôturé ne peut pas être annulé.",
          "COUNT_COMPLETED");
    }
    return inventoryCountRestMapper.toRest(requireCount(id));
  }

  public List<InventoryCountRest> list(Integer p, Integer s, String q) {
    String companyId = currentUser.requireCompanyId();
    return mapperListResponseToRest(
        inventoryCountDAO.list(p, s, q, "company_id = ?", companyId));
  }

  public InventoryCountRest one(String id) {
    return inventoryCountRestMapper.toRest(requireCount(id));
  }

  /** Comptage de l'entreprise du porteur du jeton ; un comptage étranger reste introuvable. */
  private InventoryCountResponse requireCount(String id) {
    String companyId = currentUser.requireCompanyId();
    return inventoryCountDAO
        .one(id, companyId)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Comptage d'inventaire introuvable",
                    "Aucun comptage d'inventaire ne correspond à l'identifiant " + id + ".",
                    "NOT_FOUND"));
  }

  /**
   * Réconcilie une ligne de comptage avec l'état de l'article et le solde disponible.
   *
   * <p>La contribution actuelle de l'article disponible est retirée, puis la quantité comptée est
   * ajoutée à l'entrepôt constaté. La différence et le type d'écart ont déjà été calculés par le
   * serveur lors du scan ; le client ne peut pas les imposer.
   */
  private void reconcile(
      InventoryCountScanResponse scan, InventoryCountResponse count, Timestamp completedAt) {
    ItemResponse item = itemDAO.getItemById(scan.getItem_id()).orElse(null);
    if (item == null) return;

    itemDAO.updateLastInventoryAt(item.getId(), completedAt);
    if (scan.getCounted_quantity() == null) {
      // Ligne historique antérieure au Lot 3 : on horodate l'inventaire sans inventer de quantité.
      return;
    }

    String targetWarehouse =
        scan.getActual_warehouse_id() == null
            ? count.getWarehouse_id()
            : scan.getActual_warehouse_id();
    BigDecimal counted = scan.getCounted_quantity();
    boolean wasAvailable =
        item.getStatus() == ItemStatus.AVAILABLE && item.getCurrent_warehouse_id() != null;

    if (wasAvailable) {
      stockBalanceService.adjust(
          item.getCompany_id(),
          item.getCurrent_warehouse_id(),
          item.getItem_type_id(),
          BigDecimal.ONE.negate());
    }

    ItemStatus targetStatus;
    if (counted.signum() == 0) {
      targetStatus = ItemStatus.LOST;
    } else if ("DAMAGED".equals(scan.getDiscrepancy_type())) {
      targetStatus = ItemStatus.DAMAGED;
    } else {
      targetStatus = ItemStatus.AVAILABLE;
    }

    String targetLocation = counted.signum() == 0 ? null : targetWarehouse;
    if (targetStatus == ItemStatus.AVAILABLE && targetLocation != null && counted.signum() > 0) {
      stockBalanceService.adjust(
          item.getCompany_id(), targetLocation, item.getItem_type_id(), counted);
    }

    itemDAO.updateItemStatus(item.getId(), targetStatus);
    itemDAO.updateItemLocation(item.getId(), targetLocation);
  }

  /**
   * Journalise l'écart figé du comptage dans le journal des mouvements.
   *
   * <p>Un mouvement ADJUSTMENT est écrit par ligne dont la différence est non nulle : la source est
   * l'entrepôt attendu pour une diminution, la destination est l'entrepôt constaté pour une
   * augmentation. La quantité journalisée est la valeur absolue de l'écart figé.
   */
  private void journalAdjustment(
      InventoryCountScanResponse scan,
      InventoryCountResponse count,
      Timestamp completedAt,
      String performedBy) {
    BigDecimal difference = scan.getDifference();
    if (difference == null || difference.signum() == 0) return;

    boolean increase = difference.signum() > 0;
    String warehouseId =
        increase
            ? firstNonNull(scan.getActual_warehouse_id(), count.getWarehouse_id())
            : firstNonNull(scan.getExpected_warehouse_id(), count.getWarehouse_id());
    StockMovementCreateRequest movement = new StockMovementCreateRequest();
    movement.setMovement_type(MovementType.ADJUSTMENT);
    movement.setItem_id(scan.getItem_id());
    movement.setWarehouse_id(warehouseId);
    movement.setQuantity(difference.abs());
    if (increase) {
      movement.setDestination_warehouse_id(warehouseId);
    } else {
      movement.setSource_warehouse_id(warehouseId);
    }
    movement.setReference(count.getReference());
    movement.setNotes(
        "Ajustement de comptage "
            + count.getReference()
            + (scan.getQr_code() == null ? "" : " (article " + scan.getQr_code() + ")"));
    movement.setPerformed_by(performedBy);
    movement.setMovement_date(completedAt);
    stockMovementDAO.createStockMovement(movement);
  }

  private String resolveActor(Object requested) {
    String fromToken = currentUserId();
    return fromToken != null ? fromToken : uuidOrNull(requested);
  }

  private String currentUserId() {
    try {
      JwtPrincipal principal = currentUser.require();
      String userId = principal.userId();
      return userId == null || userId.isBlank() ? null : userId;
    } catch (AccessDeniedException e) {
      return null;
    }
  }

  private String trimmed(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private String firstNonNull(String first, String second) {
    return first == null ? second : first;
  }

  private String uuidOrNull(Object value) {
    if (value == null || String.valueOf(value).isBlank()) return null;
    try {
      return UUID.fromString(String.valueOf(value)).toString();
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private List<InventoryCountRest> mapperListResponseToRest(List<InventoryCountResponse> items) {
    return items.stream().map(inventoryCountRestMapper::toRest).toList();
  }

  private List<InventoryCountScanRest> mapperListScanResponseToRest(
      List<InventoryCountScanResponse> items) {
    return items.stream().map(inventoryCountScanRestMapper::toRest).toList();
  }
}
