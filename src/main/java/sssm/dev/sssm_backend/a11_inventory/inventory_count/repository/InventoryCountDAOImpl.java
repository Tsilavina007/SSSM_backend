package sssm.dev.sssm_backend.a11_inventory.inventory_count.repository;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Repository;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ClientException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ConflictException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
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
        r.getBigDecimal("expected_quantity"),
        r.getBigDecimal("counted_quantity"),
        r.getBigDecimal("difference"),
        r.getString("discrepancy_type"),
        r.getString("notes"),
        r.getString("qr_code"));
  }

  public List<InventoryCountScanResponse> getResults(String id) {
    if (one(id).isEmpty()) {
      throw new NotFoundException(
          "Comptage d'inventaire introuvable",
          "Aucun comptage d'inventaire ne correspond à l'identifiant " + id + ".",
          "NOT_FOUND");
    }
    List<InventoryCountScanResponse> o = new ArrayList<>();
    try (Connection c = DatabaseConnection.getConnection();
        PreparedStatement p =
            c.prepareStatement(
                "SELECT s.*,i.qr_code FROM security.inventory_count_scan s "
                    + "JOIN security.item i ON i.id=s.item_id "
                    + "WHERE s.inventory_count_id=? ORDER BY s.scanned_at, s.id")) {
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
          "counted_quantity",
          "discrepancy_type",
          "notes");

  public InventoryCountScanResponse createScan(String inventoryCountId, Map<String, Object> b) {
    if (b == null || b.isEmpty())
      throw new ClientException(
          "Article obligatoire", "Le champ 'item_id' est obligatoire.", "CLIENT_ERROR");
    for (String key : b.keySet())
      if (!scanBodyColumns.contains(key))
        throw new ClientException(
            "Champ inconnu",
            "Le champ '" + key + "' n'est pas autorisé pour un scan de comptage.",
            "CLIENT_ERROR");
    Object itemId = b.get("item_id");
    if (itemId == null || String.valueOf(itemId).isBlank())
      throw new ClientException(
          "Article obligatoire", "Le champ 'item_id' est obligatoire.", "CLIENT_ERROR");

    UUID countUuid = uuid("inventory_count_id", inventoryCountId);
    UUID itemUuid = uuid("item_id", String.valueOf(itemId));

    try (Connection c = DatabaseConnection.getConnection()) {
      String companyId;
      String countWarehouseId;
      String countStatus;
      try (PreparedStatement p =
          c.prepareStatement(
              "SELECT company_id, warehouse_id, status FROM security.inventory_count WHERE id=?")) {
        p.setObject(1, countUuid);
        try (ResultSet r = p.executeQuery()) {
          if (!r.next())
            throw new NotFoundException(
                "Comptage d'inventaire introuvable",
                "Aucun comptage d'inventaire ne correspond à l'identifiant "
                    + inventoryCountId
                    + ".",
                "NOT_FOUND");
          companyId = r.getString("company_id");
          countWarehouseId = r.getString("warehouse_id");
          countStatus = r.getString("status");
        }
      }
      if (!"IN_PROGRESS".equals(countStatus))
        throw new ConflictException(
            "Comptage déjà clôturé",
            "Un scan ne peut être ajouté qu'à un comptage IN_PROGRESS.",
            "COUNT_NOT_OPEN");

      String itemCompanyId;
      String itemTypeId;
      String itemStatus;
      String expectedWarehouseId;
      try (PreparedStatement p =
          c.prepareStatement(
              "SELECT company_id, item_type_id, status, current_warehouse_id "
                  + "FROM security.item WHERE id=?")) {
        p.setObject(1, itemUuid);
        try (ResultSet r = p.executeQuery()) {
          if (!r.next())
            throw new NotFoundException(
                "Article introuvable",
                "Aucun article ne correspond à l'identifiant " + itemId + ".",
                "NOT_FOUND");
          itemCompanyId = r.getString("company_id");
          itemTypeId = r.getString("item_type_id");
          itemStatus = r.getString("status");
          expectedWarehouseId = r.getString("current_warehouse_id");
        }
      }
      if (companyId != null && itemCompanyId != null && !companyId.equals(itemCompanyId))
        throw new NotFoundException(
            "Article introuvable",
            "L'article n'appartient pas à l'entreprise de ce comptage.",
            "NOT_FOUND");

      String actualWarehouseId =
          b.get("actual_warehouse_id") == null
              ? countWarehouseId
              : String.valueOf(scannedUuid("actual_warehouse_id", b.get("actual_warehouse_id")));
      BigDecimal expected =
          expectedWarehouseId != null
                  && expectedWarehouseId.equals(countWarehouseId)
                  && "AVAILABLE".equals(itemStatus)
              ? BigDecimal.ONE
              : BigDecimal.ZERO;
      BigDecimal counted = decimal("counted_quantity", b.get("counted_quantity"), BigDecimal.ONE);
      if (counted.signum() < 0)
        throw new ClientException(
            "Quantité invalide",
            "La quantité comptée ne peut pas être négative.",
            "CLIENT_ERROR");
      BigDecimal difference = counted.subtract(expected);
      String discrepancy = discrepancy(expected, counted, expectedWarehouseId, actualWarehouseId, itemStatus);

      String sql =
          "INSERT INTO security.inventory_count_scan "
              + "(inventory_count_id, item_id, scanned_by, scanned_at, expected_warehouse_id, "
              + "actual_warehouse_id, expected_quantity, counted_quantity, difference, "
              + "discrepancy_type, notes) "
              + "VALUES (?, ?, ?, COALESCE(?, now()), ?, ?, ?, ?, ?, ?, ?) RETURNING id";
      String generatedId;
      try (PreparedStatement p = c.prepareStatement(sql)) {
        p.setObject(1, countUuid);
        p.setObject(2, itemUuid);
        p.setObject(3, nullableUuid("scanned_by", b.get("scanned_by")));
        p.setTimestamp(4, nullableTimestamp(b.get("scanned_at")));
        p.setObject(5, expectedWarehouseId == null ? null : UUID.fromString(expectedWarehouseId));
        p.setObject(6, UUID.fromString(actualWarehouseId));
        p.setBigDecimal(7, expected);
        p.setBigDecimal(8, counted);
        p.setBigDecimal(9, difference);
        p.setString(10, discrepancy);
        p.setString(11, b.get("notes") == null ? null : String.valueOf(b.get("notes")));
        try (ResultSet r = p.executeQuery()) {
          if (!r.next())
            throw new ServerException("Le scan de comptage n'a pas pu être enregistré.");
          generatedId = r.getString("id");
        }
      }
      return getScanById(generatedId);
    } catch (SQLException e) {
      if ("23505".equals(e.getSQLState()))
        throw new ClientException(
            "Article déjà scanné",
            "Cet article a déjà été scanné pour ce comptage.",
            "CLIENT_ERROR");
      if ("23503".equals(e.getSQLState()))
        throw new ClientException(
            "Référence invalide",
            "L'article, l'entrepôt ou le scanneur indiqué est introuvable ou invalide.",
            "CLIENT_ERROR");
      throw new ServerException(e);
    }
  }

  public void complete(String id, Timestamp completedAt, String completedBy, String notes) {
    String sql =
        "UPDATE security.inventory_count SET status='COMPLETED', completed_at=?, "
            + "completed_by=?, notes=COALESCE(?, notes) "
            + "WHERE id=? AND status='IN_PROGRESS'";
    try (Connection c = DatabaseConnection.getConnection();
        PreparedStatement p = c.prepareStatement(sql)) {
      p.setTimestamp(1, completedAt);
      p.setObject(2, nullableUuid("completed_by", completedBy));
      p.setString(3, notes == null || notes.isBlank() ? null : notes);
      p.setObject(4, UUID.fromString(id));
      if (p.executeUpdate() == 0)
        throw new ConflictException(
            "Comptage déjà clôturé",
            "Le comptage n'est plus IN_PROGRESS ou n'existe plus.",
            "COUNT_NOT_OPEN");
    } catch (SQLException e) {
      throw new ServerException(e);
    }
  }

  private InventoryCountScanResponse getScanById(String scanId) {
    try (Connection c = DatabaseConnection.getConnection();
        PreparedStatement p =
            c.prepareStatement(
                "SELECT s.*,i.qr_code FROM security.inventory_count_scan s "
                    + "JOIN security.item i ON i.id=s.item_id WHERE s.id=?")) {
      p.setObject(1, UUID.fromString(scanId));
      try (ResultSet r = p.executeQuery()) {
        if (r.next()) return mapRowToInventoryCountScan(r);
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    throw new ServerException(
        "Erreur interne du serveur",
        "Le scan créé n'a pas pu être relu depuis la base de données.",
        "SERVER_ERROR");
  }

  private String discrepancy(
      BigDecimal expected,
      BigDecimal counted,
      String expectedWarehouseId,
      String actualWarehouseId,
      String itemStatus) {
    if (counted.signum() == 0) return "MISSING";
    if ("DAMAGED".equals(itemStatus)) return "DAMAGED";
    if (expected.signum() == 0) return "UNEXPECTED";
    if (expectedWarehouseId != null && !expectedWarehouseId.equals(actualWarehouseId))
      return "WRONG_LOCATION";
    return "NONE";
  }

  private BigDecimal decimal(String field, Object value, BigDecimal fallback) {
    if (value == null) return fallback;
    try {
      BigDecimal parsed = new BigDecimal(String.valueOf(value));
      if (parsed.scale() > 2)
        throw new ClientException(
            "Quantité invalide",
            "Le champ '" + field + "' doit avoir au maximum 2 décimales.",
            "CLIENT_ERROR");
      return parsed;
    } catch (NumberFormatException e) {
      throw new ClientException(
          "Quantité invalide",
          "Le champ '" + field + "' doit être un nombre valide.",
          "CLIENT_ERROR");
    }
  }

  private Object nullableUuid(String field, Object value) {
    if (value == null || String.valueOf(value).isBlank()) return null;
    return scannedUuid(field, value);
  }

  private UUID scannedUuid(String field, Object value) {
    try {
      return UUID.fromString(String.valueOf(value));
    } catch (Exception e) {
      throw new ClientException(
          "Identifiant invalide",
          "Le champ '" + field + "' doit contenir un UUID valide.",
          "CLIENT_ERROR");
    }
  }

  private UUID uuid(String field, String value) {
    try {
      return UUID.fromString(String.valueOf(value));
    } catch (Exception e) {
      throw new ClientException(
          "Identifiant invalide",
          "Le champ '" + field + "' doit contenir un UUID valide.",
          "CLIENT_ERROR");
    }
  }

  private Timestamp nullableTimestamp(Object value) {
    if (value == null) return null;
    return scanTimestamp(value);
  }

  private Timestamp scanTimestamp(Object value) {
    try {
      if (value instanceof Number n) return new Timestamp(n.longValue());
      return Timestamp.from(Instant.parse(String.valueOf(value)));
    } catch (RuntimeException first) {
      try {
        return Timestamp.valueOf(LocalDateTime.parse(String.valueOf(value)));
      } catch (RuntimeException second) {
        throw new ClientException(
            "Date de scan invalide",
            "Le champ 'scanned_at' doit être une date ISO-8601 ou un timestamp valide.",
            "CLIENT_ERROR");
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
            "errors", new ItemError("Corps de requête obligatoire", "Le corps JSON de la requête est vide ou absent.", "BAD_REQUEST"));
      List<String> cs = new ArrayList<>();
      List<Object> vs = new ArrayList<>();
      for (var e : b.entrySet())
        if (e.getKey().matches("[A-Za-z_][A-Za-z0-9_]*") && !e.getKey().equals("id")) {
          cs.add(e.getKey());
          vs.add(id(e.getValue()));
        }
      if (cs.isEmpty())
        return Map.of(
            "errors", new ItemError("Aucun champ modifiable", "Le corps ne contient aucun champ autorisé pour un comptage d'inventaire.", "BAD_REQUEST"));
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
              "errors", new ItemError("Comptage d'inventaire introuvable", "Aucun comptage d'inventaire ne correspond à l'identifiant " + v + ".", "NOT_FOUND"));
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
