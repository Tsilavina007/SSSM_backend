package sssm.dev.sssm_backend;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;
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

  @Autowired private StockMovementService stockMovementService;
  @MockitoBean private ItemDAO itemDAO;

  private String itemId;
  private String warehouseId;

  @BeforeEach
  void setUp() throws Exception {
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt =
            conn.prepareStatement(
                "SELECT i.id AS item_id, w.id AS warehouse_id "
                    + "FROM security.item i CROSS JOIN security.warehouse w "
                    + "ORDER BY i.id, w.id LIMIT 1");
        ResultSet rs = stmt.executeQuery()) {
      if (rs.next()) {
        itemId = rs.getString("item_id");
        warehouseId = rs.getString("warehouse_id");
      }
    }
    Assumptions.assumeTrue(
        itemId != null && warehouseId != null, "article et entrepôt de démonstration requis");
    purge();
  }

  @AfterEach
  void cleanUp() throws Exception {
    purge();
  }

  @Test
  void movementIsRolledBackWhenItemUpdateFails() throws Exception {
    ItemResponse item = new ItemResponse();
    item.setId(itemId);
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

  private static void purge() throws Exception {
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt =
            conn.prepareStatement(
                "DELETE FROM security.stock_movement WHERE notes LIKE 'phase2-ts-tx-%'")) {
      stmt.executeUpdate();
    }
  }
}
