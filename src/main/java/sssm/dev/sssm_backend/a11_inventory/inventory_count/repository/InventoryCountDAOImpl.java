package sssm.dev.sssm_backend.a11_inventory.inventory_count.repository;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Repository;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ClientException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;
import sssm.dev.sssm_backend.a01_common.errors.ItemError;
import sssm.dev.sssm_backend.a11_inventory.inventory_count.dto.InventoryCountResponse;
import sssm.dev.sssm_backend.a11_inventory.inventory_count.dto.InventoryCountScanResponse;

/** JDBC DAO for the inventory inventory count module. */
@Repository
public class InventoryCountDAOImpl implements InventoryCountDAO {
  private static final String inventory_count = "security.inventory_count";

  private Map<String, Object> row(ResultSet r) throws SQLException {
    Map<String, Object> m = new LinkedHashMap<>();
    ResultSetMetaData d = r.getMetaData();
    for (int i = 1; i <= d.getColumnCount(); i++) m.put(d.getColumnLabel(i), r.getObject(i));
    return m;
  }

  private Object id(Object x) {
    try {
      return x == null ? null : UUID.fromString(String.valueOf(x));
    } catch (Exception e) {
      return x;
    }
  }

  private InventoryCountResponse mapRowToInventoryCount(ResultSet r) throws SQLException {
    return new InventoryCountResponse(
        r.getString("id"),
        r.getString("company_id"),
        r.getString("warehouse_id"),
        r.getString("reference"),
        r.getString("status"),
        r.getString("started_by"),
        r.getString("completed_by"),
        r.getTimestamp("started_at"),
        r.getTimestamp("completed_at"),
        r.getString("notes"));
  }

  private InventoryCountScanResponse mapRowToInventoryCountScan(ResultSet r) throws SQLException {
    return new InventoryCountScanResponse(
        r.getString("id"),
        r.getString("inventory_count_id"),
        r.getString("item_id"),
        r.getString("scanned_by"),
        r.getTimestamp("scanned_at"),
        r.getString("expected_warehouse_id"),
        r.getString("actual_warehouse_id"),
        r.getString("discrepancy_type"),
        r.getString("notes"),
        r.getString("qr_code"));
  }

  public List<InventoryCountScanResponse> getResults(String id) {
    List<InventoryCountScanResponse> o = new ArrayList<>();
    try (Connection c = DatabaseConnection.getConnection();
        PreparedStatement p =
            c.prepareStatement(
                "SELECT s.*,i.qr_code FROM security.inventory_count_scan s JOIN security.item i ON i.id=s.item_id WHERE s.inventory_count_id=?")) {
      p.setObject(1, this.id(id));
      try (ResultSet r = p.executeQuery()) {
        while (r.next()) o.add(mapRowToInventoryCountScan(r));
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return o;
  }

  private static final Set<String> scanBodyColumns =
      Set.of(
          "inventory_count_id",
          "item_id",
          "scanned_by",
          "scanned_at",
          "expected_warehouse_id",
          "actual_warehouse_id",
          "discrepancy_type",
          "notes");
  private static final Set<String> scanUuidColumns =
      Set.of("item_id", "scanned_by", "expected_warehouse_id", "actual_warehouse_id");
  private static final Set<String> scanDiscrepancyTypes =
      Set.of("NONE", "MISSING", "UNEXPECTED", "WRONG_LOCATION", "DAMAGED");

  public InventoryCountScanResponse createScan(String inventoryCountId, Map<String, Object> b) {
    if (b == null || b.isEmpty()) throw new ClientException("item_id is required");
    for (String key : b.keySet())
      if (!scanBodyColumns.contains(key)) throw new ClientException("unknown field: " + key);
    Object itemId = b.get("item_id");
    if (itemId == null || String.valueOf(itemId).isBlank())
      throw new ClientException("item_id is required");
    Object discrepancy = b.get("discrepancy_type");
    if (discrepancy != null && !scanDiscrepancyTypes.contains(String.valueOf(discrepancy)))
      throw new ClientException("invalid discrepancy_type");

    // inventory_count_id provient toujours du chemin ; scanned_at absent → défaut now().
    List<String> cs = new ArrayList<>(List.of("inventory_count_id"));
    List<Object> vs = new ArrayList<>(List.of(UUID.fromString(inventoryCountId)));
    for (var e : b.entrySet()) {
      if (e.getValue() == null || e.getKey().equals("inventory_count_id")) continue;
      cs.add(e.getKey());
      if (e.getKey().equals("scanned_at")) vs.add(scanTimestamp(e.getValue()));
      else if (scanUuidColumns.contains(e.getKey())) vs.add(scanUuid(e.getKey(), e.getValue()));
      else vs.add(String.valueOf(e.getValue()));
    }
    StringJoiner cols = new StringJoiner(","), qs = new StringJoiner(",");
    for (String x : cs) {
      cols.add(x);
      qs.add("?");
    }
    String sql = "INSERT INTO security.inventory_count_scan (" + cols + ") VALUES (" + qs + ")";
    String generatedId = null;
    try (Connection c = DatabaseConnection.getConnection();
        PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      int n = 1;
      for (Object x : vs) p.setObject(n++, x);
      p.executeUpdate();
      try (ResultSet r = p.getGeneratedKeys()) {
        if (r.next()) generatedId = String.valueOf(r.getObject(1));
      }
    } catch (SQLException e) {
      if ("23505".equals(e.getSQLState()))
        throw new ClientException("item already scanned for this count");
      if ("23503".equals(e.getSQLState()))
        throw new ClientException("invalid item, warehouse or scanner reference");
      throw new ServerException(e);
    }
    return getScanById(generatedId);
  }

  private InventoryCountScanResponse getScanById(String scanId) {
    try (Connection c = DatabaseConnection.getConnection();
        PreparedStatement p =
            c.prepareStatement(
                "SELECT s.*,i.qr_code FROM security.inventory_count_scan s JOIN security.item i ON i.id=s.item_id WHERE s.id=?")) {
      p.setObject(1, UUID.fromString(scanId));
      try (ResultSet r = p.executeQuery()) {
        if (r.next()) return mapRowToInventoryCountScan(r);
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    throw new ServerException("scan not found after insert");
  }

  private Object scanUuid(String field, Object value) {
    try {
      return UUID.fromString(String.valueOf(value));
    } catch (Exception e) {
      throw new ClientException("invalid " + field);
    }
  }

  private Timestamp scanTimestamp(Object value) {
    try {
      if (value instanceof Number n) return new Timestamp(n.longValue());
      return Timestamp.from(Instant.parse(String.valueOf(value)));
    } catch (RuntimeException first) {
      try {
        return Timestamp.valueOf(LocalDateTime.parse(String.valueOf(value)));
      } catch (RuntimeException second) {
        throw new ClientException("invalid scanned_at");
      }
    }
  }

  public List<InventoryCountResponse> list(
      Integer page, Integer size, String search, String where, Object... a) {
    List<InventoryCountResponse> o = new ArrayList<>();
    int p = page == null ? 1 : Math.max(1, page),
        s = size == null ? 50 : Math.min(500, Math.max(1, size));
    String q =
        "SELECT * FROM "
            + inventory_count
            + " WHERE "
            + (where == null ? "1=1" : where)
            + (search == null || search.isBlank()
                ? ""
                : " AND CAST(row_to_json(inventory_count) AS text) ILIKE ?")
            + " ORDER BY id LIMIT ? OFFSET ?";
    try (Connection c = DatabaseConnection.getConnection();
        PreparedStatement z = c.prepareStatement(q)) {
      int n = 1;
      for (Object x : a) z.setObject(n++, id(x));
      if (search != null && !search.isBlank()) z.setString(n++, "%" + search + "%");
      z.setInt(n++, s);
      z.setInt(n, (p - 1) * s);
      try (ResultSet r = z.executeQuery()) {
        while (r.next()) o.add(mapRowToInventoryCount(r));
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return o;
  }

  public Optional<InventoryCountResponse> one(String v) {
    try (Connection c = DatabaseConnection.getConnection();
        PreparedStatement p =
            c.prepareStatement("SELECT * FROM " + inventory_count + " WHERE id=?")) {
      p.setObject(1, id(v));
      try (ResultSet r = p.executeQuery()) {
        return r.next() ? Optional.of(mapRowToInventoryCount(r)) : Optional.empty();
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
  }

  public Object save(String v, Map<String, Object> b, boolean u) {
    try (Connection c = DatabaseConnection.getConnection()) {
      if (b == null || b.isEmpty())
        return Map.of(
            "errors", new ItemError("Request body is required", null, "BAD_REQUEST"));
      List<String> cs = new ArrayList<>();
      List<Object> vs = new ArrayList<>();
      for (var e : b.entrySet())
        if (e.getKey().matches("[A-Za-z_][A-Za-z0-9_]*") && !e.getKey().equals("id")) {
          cs.add(e.getKey());
          vs.add(id(e.getValue()));
        }
      if (cs.isEmpty())
        return Map.of(
            "errors", new ItemError("No writable fields", null, "BAD_REQUEST"));
      StringJoiner set = new StringJoiner(","),
          cols = new StringJoiner(","),
          qs = new StringJoiner(",");
      for (String x : cs) {
        set.add(x + "=?");
        cols.add(x);
        qs.add("?");
      }
      String sql =
          u
              ? "UPDATE " + inventory_count + " SET " + set + " WHERE id=?"
              : "INSERT INTO " + inventory_count + " (" + cols + ") VALUES (" + qs + ")";
      try (PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
        int n = 1;
        for (Object x : vs) p.setObject(n++, x);
        if (u) p.setObject(n, id(v));
        p.executeUpdate();
        if (!u)
          try (ResultSet r = p.getGeneratedKeys()) {
            if (r.next()) v = String.valueOf(r.getObject(1));
          }
      }
      InventoryCountResponse r = one(v).orElse(null);
      return r != null
          ? r
          : Map.of(
              "errors", new ItemError("Not found", "id: " + v, "NOT_FOUND"));
    } catch (SQLException e) {
      throw new ServerException(e);
    }
  }

  public Map<String, Object> delete(String v) {
    try (Connection c = DatabaseConnection.getConnection();
        PreparedStatement p =
            c.prepareStatement("DELETE FROM " + inventory_count + " WHERE id=?")) {
      p.setObject(1, id(v));
      return Map.of("id", v, "deleted", p.executeUpdate() > 0);
    } catch (SQLException e) {
      throw new ServerException(e);
    }
  }

  public List<Map<String, Object>> query(String q, Object... a) {
    List<Map<String, Object>> o = new ArrayList<>();
    try (Connection c = DatabaseConnection.getConnection();
        PreparedStatement p = c.prepareStatement(q)) {
      for (int i = 0; i < a.length; i++) p.setObject(i + 1, id(a[i]));
      try (ResultSet r = p.executeQuery()) {
        while (r.next()) o.add(row(r));
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return o;
  }
}
