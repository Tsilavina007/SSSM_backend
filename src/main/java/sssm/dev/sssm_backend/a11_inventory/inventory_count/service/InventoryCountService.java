package sssm.dev.sssm_backend.a11_inventory.inventory_count.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sssm.dev.sssm_backend.a00_infrastructure.exception.ConflictException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a01_common.enums.ItemStatus;
import sssm.dev.sssm_backend.a01_common.errors.ItemError;
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

@Service
@RequiredArgsConstructor
public class InventoryCountService {
  private final InventoryCountDAO inventoryCountDAO;
  private final InventoryCountRestMapper inventoryCountRestMapper;
  private final InventoryCountScanRestMapper inventoryCountScanRestMapper;
  private final ItemDAO itemDAO;
  private final StockBalanceService stockBalanceService;

  public List<InventoryCountScanRest> getResults(String id) {
    return mapperListScanResponseToRest(inventoryCountDAO.getResults(id));
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

  @Transactional
  public Object close(String id, Map<String, Object> b) {
    InventoryCountResponse count = requireCount(id);
    if (!"IN_PROGRESS".equals(count.getStatus())) {
      throw new ConflictException(
          "Comptage déjà clôturé",
          "Seul un comptage IN_PROGRESS peut être clôturé.",
          "COUNT_NOT_OPEN");
    }

    Timestamp completedAt = Timestamp.from(Instant.now());
    String completedBy = uuidOrNull(b == null ? null : b.get("completed_by"));
    String notes = b == null || b.get("notes") == null ? null : String.valueOf(b.get("notes"));

    List<InventoryCountScanResponse> scans = inventoryCountDAO.getResults(id);
    for (InventoryCountScanResponse scan : scans) {
      reconcile(scan, count, completedAt);
    }

    inventoryCountDAO.complete(id, completedAt, completedBy, notes);
    return inventoryCountRestMapper.toRest(requireCount(id));
  }

  public List<InventoryCountRest> list(Integer p, Integer s, String q, String w, Object... a) {
    return mapperListResponseToRest(inventoryCountDAO.list(p, s, q, w, a));
  }

  public Object one(String id) {
    return inventoryCountRestMapper.toRest(requireCount(id));
  }

  @Transactional
  public Object save(String id, Map<String, Object> b, boolean u) {
    Object r = inventoryCountDAO.save(id, b, u);
    return r instanceof InventoryCountResponse rs ? inventoryCountRestMapper.toRest(rs) : r;
  }

  public Map<String, Object> delete(String id) {
    return inventoryCountDAO.delete(id);
  }

  public List<Map<String, Object>> query(String q, Object... a) {
    return inventoryCountDAO.query(q, a);
  }

  private InventoryCountResponse requireCount(String id) {
    return inventoryCountDAO
        .one(id)
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
