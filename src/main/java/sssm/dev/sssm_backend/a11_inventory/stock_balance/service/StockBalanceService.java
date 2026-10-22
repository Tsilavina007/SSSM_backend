package sssm.dev.sssm_backend.a11_inventory.stock_balance.service;

import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a01_common.enums.ItemStatus;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemResponse;
import sssm.dev.sssm_backend.a11_inventory.stock_balance.dto.StockBalanceResponse;
import sssm.dev.sssm_backend.a11_inventory.stock_balance.dto.mapper.StockBalanceRestMapper;
import sssm.dev.sssm_backend.a11_inventory.stock_balance.dto.rest.StockBalanceRest;
import sssm.dev.sssm_backend.a11_inventory.stock_balance.repository.StockBalanceDAO;

/**
 * Source de vérité du stock disponible.
 *
 * <p>Les mouvements mettent à jour ce solde dans la transaction du mouvement. Les créations,
 * modifications et suppressions directes d'articles synchronisent également leur contribution
 * unitaire, afin que les routes historiques ne désynchronisent pas le journal.
 */
@Service
@RequiredArgsConstructor
public class StockBalanceService {
  private final StockBalanceDAO stockBalanceDAO;
  private final StockBalanceRestMapper stockBalanceRestMapper;

  public BigDecimal adjust(
      String companyId, String warehouseId, String itemTypeId, BigDecimal delta) {
    return stockBalanceDAO.adjust(companyId, warehouseId, itemTypeId, delta);
  }

  public BigDecimal quantity(String companyId, String warehouseId, String itemTypeId) {
    return stockBalanceDAO.quantity(companyId, warehouseId, itemTypeId);
  }

  public List<StockBalanceRest> list(
      String warehouseId, String itemTypeId, Integer page, Integer pageSize) {
    List<StockBalanceResponse> balances =
        stockBalanceDAO.list(warehouseId, itemTypeId, page, pageSize);
    return balances.stream().map(stockBalanceRestMapper::toRest).toList();
  }

  /**
   * Synchronise la contribution d'un article unitaire au stock disponible lors d'une création,
   * modification ou suppression directe. Un article affecté, perdu, retiré, endommagé ou en
   * réparation ne contribue pas au stock disponible.
   */
  public void syncItem(ItemResponse before, ItemResponse after) {
    if (before != null && contributes(before)) {
      if (after != null && contributes(after) && sameBalanceKey(before, after)) {
        return;
      }
      adjust(
          before.getCompany_id(),
          before.getCurrent_warehouse_id(),
          before.getItem_type_id(),
          BigDecimal.ONE.negate());
    }
    if (after != null && contributes(after)) {
      adjust(
          after.getCompany_id(),
          after.getCurrent_warehouse_id(),
          after.getItem_type_id(),
          BigDecimal.ONE);
    }
  }

  private boolean contributes(ItemResponse item) {
    return item != null
        && item.getStatus() == ItemStatus.AVAILABLE
        && item.getCurrent_warehouse_id() != null
        && item.getArchived_at() == null;
  }

  private boolean sameBalanceKey(ItemResponse before, ItemResponse after) {
    return before.getCompany_id().equals(after.getCompany_id())
        && before.getCurrent_warehouse_id().equals(after.getCurrent_warehouse_id())
        && before.getItem_type_id().equals(after.getItem_type_id());
  }
}
