package sssm.dev.sssm_backend;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;
import sssm.dev.sssm_backend.a01_common.enums.ItemStatus;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemResponse;
import sssm.dev.sssm_backend.a11_inventory.item.repository.ItemDAO;
import sssm.dev.sssm_backend.a11_inventory.stock_movement.dto.StockMovementCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.stock_movement.service.StockMovementService;

/**
 * Vérifie qu'un échec après l'insertion du mouvement annule bien tout le cas d'usage service.
 */
@SpringBootTest
class StockMovementTransactionTests {

  private static final String TYPE_CODE = "phase2-tx-type";
  private static final String ITEM_QR = "phase2-tx-item";

  @Autowired private StockMovementService stockMovementService;
  @MockitoBean private ItemDAO itemDAO;

  private String companyId;
  private String itemTypeId;
  private String itemId;
  private String warehouseId;

  @BeforeEach
  void setUp() throws Exception {
    try (Connection conn = DatabaseConnection.getConnection()) {
      companyId = firstId(conn, "SELECT id FROM security.company ORDER BY id LIMIT 1");
      warehouseId = firstId(conn, "SELECT id FROM security.warehouse ORDER BY id LIMIT 1");
      Assumptions.assumeTrue(
          companyId != null && warehouseId != null, "entreprise et entrepôt de démonstration requis");
      purge();
      itemTypeId = insertItemType(conn);
      itemId = insertItem(conn);
      insertBalance(conn);
    }
  }

  @AfterEach
  void cleanUp() throws Exception {
    purge();
  }

  @Test
  void movementIsRolledBackWhenItemUpdateFails() throws Exception {
    ItemResponse item = new ItemResponse();
    item.setId(itemId);
    item.setCompany_id(companyId);
    item.setItem_type_id(itemTypeId);
    item.setStatus(ItemStatus.AVAILABLE);
    when(itemDAO.getItemById(itemId)).thenReturn(Optional.of(item));
    doThrow(new ServerException("échec forcé après l'insertion du mouvement"))
        .when(itemDAO)
        .updateItemStatus(eq(itemId), any(ItemStatus.class));

    String marker = "phase2-ts-tx-" + System.nanoTime();
    StockMovementCreateRequest request = new StockMovementCreateRequest();
    request.setWarehouse_id(warehouseId);
    request.setItem_id(itemId);
    request.setNotes(marker);

    assertThrows(ServerException.class, () -> stockMovementService.createStockMovement(request));
    assertFalse(movementExists(marker), "le mouvement doit être annulé avec la transaction");
    assertFalse(
        balanceWasDuplicated(companyId, warehouseId, itemTypeId),
        "le solde doit être restauré par le rollback");
  }

  private String insertItemType(Connection conn) throws Exception {
    try (PreparedStatement stmt =
        conn.prepareStatement(
            "INSERT INTO security.item_type (company_id, code, name) VALUES (?, ?, ?) RETURNING id")) {
      stmt.setObject(1, UUID.fromString(companyId));
      stmt.setString(2, TYPE_CODE);
      stmt.setString(3, TYPE_CODE);
      try (ResultSet rs = stmt.executeQuery()) {
        rs.next();
        return rs.getString(1);
      }
    }
  }

  private String insertItem(Connection conn) throws Exception {
    try (PreparedStatement stmt =
        conn.prepareStatement(
            "INSERT INTO security.item "
                + "(company_id, item_type_id, qr_code, status, current_warehouse_id) "
                + "VALUES (?, ?, ?, 'AVAILABLE', ?) RETURNING id")) {
      stmt.setObject(1, UUID.fromString(companyId));
      stmt.setObject(2, UUID.fromString(itemTypeId));
      stmt.setString(3, ITEM_QR);
      stmt.setObject(4, UUID.fromString(warehouseId));
      try (ResultSet rs = stmt.executeQuery()) {
        rs.next();
        return rs.getString(1);
      }
    }
  }

  private void insertBalance(Connection conn) throws Exception {
    try (PreparedStatement stmt =
        conn.prepareStatement(
            "INSERT INTO security.stock_balance "
                + "(company_id, warehouse_id, item_type_id, quantity) VALUES (?, ?, ?, ?)")) {
      stmt.setObject(1, UUID.fromString(companyId));
      stmt.setObject(2, UUID.fromString(warehouseId));
      stmt.setObject(3, UUID.fromString(itemTypeId));
      stmt.setBigDecimal(4, BigDecimal.ONE);
      stmt.executeUpdate();
    }
  }

  private static boolean movementExists(String notes) throws Exception {
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt =
            conn.prepareStatement("SELECT 1 FROM security.stock_movement WHERE notes = ?")) {
      stmt.setString(1, notes);
      try (ResultSet rs = stmt.executeQuery()) {
        return rs.next();
      }
    }
  }

  private static boolean balanceWasDuplicated(
      String companyId, String warehouseId, String itemTypeId) throws Exception {
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt =
            conn.prepareStatement(
                "SELECT quantity FROM security.stock_balance "
                    + "WHERE company_id=? AND warehouse_id=? AND item_type_id=?")) {
      stmt.setObject(1, UUID.fromString(companyId));
      stmt.setObject(2, UUID.fromString(warehouseId));
      stmt.setObject(3, UUID.fromString(itemTypeId));
      try (ResultSet rs = stmt.executeQuery()) {
        return !rs.next() || rs.getBigDecimal(1).compareTo(BigDecimal.ONE) != 0;
      }
    }
  }

  private static void purge() throws Exception {
    execute("DELETE FROM security.stock_movement WHERE notes LIKE 'phase2-ts-tx-%'");
    execute(
        "DELETE FROM security.stock_balance WHERE item_type_id IN "
            + "(SELECT id FROM security.item_type WHERE code = '" + TYPE_CODE + "')");
    execute("DELETE FROM security.item WHERE qr_code = '" + ITEM_QR + "'");
    execute("DELETE FROM security.item_type WHERE code = '" + TYPE_CODE + "'");
  }

  private static String firstId(Connection conn, String sql) throws Exception {
    try (PreparedStatement stmt = conn.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()) {
      return rs.next() ? rs.getString(1) : null;
    }
  }

  private static void execute(String sql) throws Exception {
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.executeUpdate();
    }
  }
}
