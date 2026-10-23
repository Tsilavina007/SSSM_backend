package sssm.dev.sssm_backend.a11_inventory.stock_balance.repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ConflictException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;
import sssm.dev.sssm_backend.a11_inventory.stock_balance.dto.StockBalanceResponse;

@Repository
public class StockBalanceDAOImpl implements StockBalanceDAO {

  @Override
  public BigDecimal adjust(
      String companyId, String warehouseId, String itemTypeId, BigDecimal delta) {
    if (delta == null || delta.signum() == 0) {
      return quantity(companyId, warehouseId, itemTypeId);
    }

    UUID company = uuid("company_id", companyId);
    UUID warehouse = uuid("warehouse_id", warehouseId);
    UUID itemType = uuid("item_type_id", itemTypeId);

    if (delta.signum() > 0) {
      String sql =
          "INSERT INTO security.stock_balance "
              + "(company_id, warehouse_id, item_type_id, quantity) VALUES (?, ?, ?, ?) "
              + "ON CONFLICT (company_id, warehouse_id, item_type_id) DO UPDATE SET "
              + "quantity = security.stock_balance.quantity + EXCLUDED.quantity, "
              + "version = security.stock_balance.version + 1, updated_at = now() "
              + "RETURNING quantity";
      try (Connection conn = DatabaseConnection.getConnection();
          PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setObject(1, company);
        stmt.setObject(2, warehouse);
        stmt.setObject(3, itemType);
        stmt.setBigDecimal(4, delta);
        try (ResultSet rs = stmt.executeQuery()) {
          if (rs.next()) return rs.getBigDecimal("quantity");
        }
        throw new ServerException("Le solde de stock n'a pas pu être enregistré.");
      } catch (SQLException e) {
        throw new ServerException(e);
      }
    }

    String sql =
        "UPDATE security.stock_balance SET quantity = quantity + ?, "
            + "version = version + 1, updated_at = now() "
            + "WHERE company_id = ? AND warehouse_id = ? AND item_type_id = ? "
            + "AND quantity + ? >= 0 RETURNING quantity";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setBigDecimal(1, delta);
      stmt.setObject(2, company);
      stmt.setObject(3, warehouse);
      stmt.setObject(4, itemType);
      stmt.setBigDecimal(5, delta);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) return rs.getBigDecimal("quantity");
      }
      throw new ConflictException(
          "Stock insuffisant",
          "Le mouvement ferait passer le stock disponible sous zéro pour cet entrepôt et ce type d'article.",
          "INSUFFICIENT_STOCK");
    } catch (SQLException e) {
      throw new ServerException(e);
    }
  }

  @Override
  public BigDecimal quantity(String companyId, String warehouseId, String itemTypeId) {
    String sql =
        "SELECT quantity FROM security.stock_balance "
            + "WHERE company_id = ? AND warehouse_id = ? AND item_type_id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, uuid("company_id", companyId));
      stmt.setObject(2, uuid("warehouse_id", warehouseId));
      stmt.setObject(3, uuid("item_type_id", itemTypeId));
      try (ResultSet rs = stmt.executeQuery()) {
        return rs.next() ? rs.getBigDecimal("quantity") : BigDecimal.ZERO;
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
  }

  @Override
  public long count(String warehouseId, String itemTypeId) {
    StringBuilder sql = new StringBuilder("SELECT count(*) FROM security.stock_balance b WHERE 1=1 ");
    List<Object> parameters = new ArrayList<>();
    if (warehouseId != null && !warehouseId.isBlank()) {
      sql.append("AND b.warehouse_id = ? ");
      parameters.add(uuid("warehouse_id", warehouseId));
    }
    if (itemTypeId != null && !itemTypeId.isBlank()) {
      sql.append("AND b.item_type_id = ? ");
      parameters.add(uuid("item_type_id", itemTypeId));
    }
    try (Connection c = DatabaseConnection.getConnection();
        PreparedStatement s = c.prepareStatement(sql.toString())) {
      for (int i = 0; i < parameters.size(); i++) {
        s.setObject(i + 1, parameters.get(i));
      }
      try (ResultSet r = s.executeQuery()) {
        return r.next() ? r.getLong(1) : 0L;
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
  }

  @Override
  public List<StockBalanceResponse> list(
      String warehouseId, String itemTypeId, Integer page, Integer pageSize) {
    int p = page == null ? 1 : Math.max(1, page);
    int s = pageSize == null ? 50 : Math.min(500, Math.max(1, pageSize));
    StringBuilder sql =
        new StringBuilder(
            "SELECT b.*, w.name AS warehouse_name, t.code AS item_type_code, "
                + "t.name AS item_type_name, "
                + "COALESCE(c.minimum_stock, t.minimum_stock) AS minimum_stock, "
                + "(b.quantity < COALESCE(c.minimum_stock, t.minimum_stock)) AS below_minimum "
                + "FROM security.stock_balance b "
                + "JOIN security.warehouse w ON w.id = b.warehouse_id "
                + "JOIN security.item_type t ON t.id = b.item_type_id "
                + "LEFT JOIN security.warehouse_stock_config c "
                + "ON c.warehouse_id = b.warehouse_id AND c.item_type_id = b.item_type_id "
                + "WHERE 1=1 ");
    List<Object> parameters = new ArrayList<>();
    if (warehouseId != null && !warehouseId.isBlank()) {
      sql.append("AND b.warehouse_id = ? ");
      parameters.add(uuid("warehouse_id", warehouseId));
    }
    if (itemTypeId != null && !itemTypeId.isBlank()) {
      sql.append("AND b.item_type_id = ? ");
      parameters.add(uuid("item_type_id", itemTypeId));
    }
    sql.append("ORDER BY t.code, w.name LIMIT ? OFFSET ?");

    List<StockBalanceResponse> values = new ArrayList<>();
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
      int index = 1;
      for (Object parameter : parameters) stmt.setObject(index++, parameter);
      stmt.setInt(index++, s);
      stmt.setInt(index, (p - 1) * s);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) values.add(mapRow(rs));
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return values;
  }

  private StockBalanceResponse mapRow(ResultSet rs) throws SQLException {
    return new StockBalanceResponse(
        rs.getString("id"),
        rs.getString("company_id"),
        rs.getString("warehouse_id"),
        rs.getString("warehouse_name"),
        rs.getString("item_type_id"),
        rs.getString("item_type_code"),
        rs.getString("item_type_name"),
        rs.getBigDecimal("quantity"),
        rs.getBigDecimal("minimum_stock"),
        rs.getBoolean("below_minimum"),
        rs.getLong("version"),
        rs.getTimestamp("updated_at"));
  }

  private UUID uuid(String field, String value) {
    if (value == null || value.isBlank()) {
      throw new ConflictException(
          "Identifiant manquant",
          "Le champ '" + field + "' est obligatoire pour calculer le stock.",
          "MISSING_IDENTIFIER");
    }
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException e) {
      throw new ConflictException(
          "Identifiant invalide",
          "Le champ '" + field + "' doit contenir un UUID valide.",
          "INVALID_IDENTIFIER");
    }
  }
}
