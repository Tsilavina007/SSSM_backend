package sssm.dev.sssm_backend.a11_inventory.inventory_count.repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Repository;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ClientException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ConflictException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;
import sssm.dev.sssm_backend.a11_inventory.inventory_count.dto.InventoryCountResponse;
import sssm.dev.sssm_backend.a11_inventory.inventory_count.dto.InventoryCountScanResponse;

/** JDBC DAO for the inventory count module. */
@Repository
public class InventoryCountDAOImpl implements InventoryCountDAO {
  private static final String inventory_count = "security.inventory_count";
  private static final int MAX_PAGE_SIZE = 500;

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

  private static final String RESULTS_SQL =
      "SELECT s.*, i.qr_code FROM security.inventory_count_scan s "
          + "JOIN security.item i ON i.id = s.item_id "
          + "WHERE s.inventory_count_id = ? ORDER BY s.scanned_at, s.id";

  @Override
  public List<InventoryCountScanResponse> getResults(String id, int page, int pageSize) {
    int safePage = Math.max(1, page);
    int safeSize = Math.min(MAX_PAGE_SIZE, Math.max(1, pageSize));
    List<InventoryCountScanResponse> results = new ArrayList<>();
    try (Connection c = DatabaseConnection.getConnection();
        PreparedStatement p = c.prepareStatement(RESULTS_SQL + " LIMIT ? OFFSET ?")) {
      p.setObject(1, this.id(id));
      p.setInt(2, safeSize);
      p.setInt(3, (safePage - 1) * safeSize);
      try (ResultSet r = p.executeQuery()) {
        while (r.next()) results.add(mapRowToInventoryCountScan(r));
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return results;
  }

  @Override
  public int countResults(String id) {
    try (Connection c = DatabaseConnection.getConnection();
        PreparedStatement p =
            c.prepareStatement(
                "SELECT count(*) FROM security.inventory_count_scan WHERE inventory_count_id = ?")) {
      p.setObject(1, this.id(id));
      try (ResultSet r = p.executeQuery()) {
        return r.next() ? r.getInt(1) : 0;
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
  }

  @Override
  public List<InventoryCountScanResponse> getAllResults(String id) {
    List<InventoryCountScanResponse> results = new ArrayList<>();
    try (Connection c = DatabaseConnection.getConnection();
        PreparedStatement p = c.prepareStatement(RESULTS_SQL)) {
      p.setObject(1, this.id(id));
      try (ResultSet r = p.executeQuery()) {
        while (r.next()) results.add(mapRowToInventoryCountScan(r));
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return results;
  }

  @Override
  public InventoryCountResponse create(
      String companyId, String warehouseId, String reference, String startedBy, String notes) {
    try (Connection c = DatabaseConnection.getConnection()) {
      String generatedId;
      try (PreparedStatement p =
          c.prepareStatement(
              "INSERT INTO security.inventory_count "
                  + "(company_id, warehouse_id, reference, status, started_by, started_at, notes) "
                  + "VALUES (?, ?, ?, 'IN_PROGRESS', ?, now(), ?) RETURNING id")) {
        p.setObject(1, UUID.fromString(companyId));
        p.setObject(2, UUID.fromString(warehouseId));
        p.setString(3, reference);
        p.setObject(4, nullableUuid("started_by", startedBy));
        p.setString(5, notes);
        try (ResultSet r = p.executeQuery()) {
          if (!r.next()) {
            throw new ServerException("Le comptage d'inventaire n'a pas pu être créé.");
          }
          generatedId = r.getString("id");
        }
      }
      snapshotExpected(c, generatedId, companyId, warehouseId);
      return one(c, generatedId)
          .orElseThrow(
              () -> new ServerException("Le comptage créé n'a pas pu être relu."));
    } catch (SQLException e) {
      if ("23505".equals(e.getSQLState())) {
        throw new ConflictException(
            "Référence déjà utilisée",
            "Un comptage de l'entreprise utilise déjà la référence " + reference + ".",
            "COUNT_REFERENCE_EXISTS");
      }
      throw new ServerException(e);
    }
  }

  /**
   * Fige les attendus à l'ouverture : tous les articles non archivés de l'entreprise, avec la
   * quantité attendue dans l'entrepôt compté et leur emplacement théorique.
   */
  private void snapshotExpected(
      Connection connection, String countId, String companyId, String warehouseId)
      throws SQLException {
    String sql =
        "INSERT INTO security.inventory_count_expected "
            + "(inventory_count_id, item_id, company_id, expected_warehouse_id, "
            + "expected_quantity, item_status_at_open, snapshot_at) "
            + "SELECT ?, i.id, i.company_id, i.current_warehouse_id, "
            + "CASE WHEN i.status = 'AVAILABLE' AND i.current_warehouse_id = ? THEN 1 ELSE 0 END, "
            + "i.status, now() "
            + "FROM security.item i "
            + "WHERE i.company_id = ? AND i.archived_at IS NULL "
            + "ON CONFLICT (inventory_count_id, item_id) DO NOTHING";
    try (PreparedStatement p = connection.prepareStatement(sql)) {
      UUID countUuid = UUID.fromString(countId);
      UUID warehouseUuid = UUID.fromString(warehouseId);
      UUID companyUuid = UUID.fromString(companyId);
      p.setObject(1, countUuid);
      p.setObject(2, warehouseUuid);
      p.setObject(3, companyUuid);
      p.executeUpdate();
    }
  }

  @Override
  public boolean warehouseBelongsToCompany(String companyId, String warehouseId) {
    try (Connection c = DatabaseConnection.getConnection();
        PreparedStatement p =
            c.prepareStatement(
                "SELECT 1 FROM security.warehouse "
                    + "WHERE id = ? AND company_id = ? AND archived_at IS NULL")) {
      p.setObject(1, UUID.fromString(warehouseId));
      p.setObject(2, UUID.fromString(companyId));
      try (ResultSet r = p.executeQuery()) {
        return r.next();
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
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

  @Override
  public InventoryCountScanResponse createScan(String inventoryCountId, Map<String, Object> b) {
    if (b == null || b.isEmpty()) {
      throw new ClientException(
          "Article obligatoire", "Le champ 'item_id' est obligatoire.", "CLIENT_ERROR");
    }
    for (String key : b.keySet()) {
      if (!scanBodyColumns.contains(key)) {
        throw new ClientException(
            "Champ inconnu",
            "Le champ '" + key + "' n'est pas autorisé pour un scan de comptage.",
            "CLIENT_ERROR");
      }
    }
    Object itemId = b.get("item_id");
    if (itemId == null || String.valueOf(itemId).isBlank()) {
      throw new ClientException(
          "Article obligatoire", "Le champ 'item_id' est obligatoire.", "CLIENT_ERROR");
    }

    UUID countUuid = uuid("inventory_count_id", inventoryCountId);
    UUID itemUuid = uuid("item_id", String.valueOf(itemId));

    try (Connection c = DatabaseConnection.getConnection()) {
      String companyId;
      String countWarehouseId;
      String countStatus;
      try (PreparedStatement p =
          c.prepareStatement(
              "SELECT company_id, warehouse_id, status FROM security.inventory_count WHERE id = ?")) {
        p.setObject(1, countUuid);
        try (ResultSet r = p.executeQuery()) {
          if (!r.next()) {
            throw new NotFoundException(
                "Comptage d'inventaire introuvable",
                "Aucun comptage d'inventaire ne correspond à l'identifiant "
                    + inventoryCountId
                    + ".",
                "NOT_FOUND");
          }
          companyId = r.getString("company_id");
          countWarehouseId = r.getString("warehouse_id");
          countStatus = r.getString("status");
        }
      }
      if (!"IN_PROGRESS".equals(countStatus)) {
        throw new ConflictException(
            "Comptage déjà clôturé",
            "Un scan ne peut être ajouté qu'à un comptage IN_PROGRESS.",
            "COUNT_NOT_OPEN");
      }

      // L'attendu est lu dans l'instantané figé à l'ouverture : un article absent
      // de cet instantané (autre entreprise, archivé ou créé après l'ouverture)
      // est hors périmètre.
      String expectedWarehouseId;
      BigDecimal expected;
      String itemStatus;
      try (PreparedStatement p =
          c.prepareStatement(
              "SELECT e.expected_warehouse_id, e.expected_quantity, i.status AS item_status "
                  + "FROM security.inventory_count_expected e "
                  + "JOIN security.item i ON i.id = e.item_id "
                  + "WHERE e.inventory_count_id = ? AND e.item_id = ?")) {
        p.setObject(1, countUuid);
        p.setObject(2, itemUuid);
        try (ResultSet r = p.executeQuery()) {
          if (!r.next()) {
            throw new ConflictException(
                "Article hors périmètre",
                "L'article n'appartient pas au périmètre figé de ce comptage "
                    + "(autre entreprise, article archivé ou créé après l'ouverture).",
                "ITEM_OUT_OF_SCOPE");
          }
          expectedWarehouseId = r.getString("expected_warehouse_id");
          expected = r.getBigDecimal("expected_quantity");
          itemStatus = r.getString("item_status");
        }
      }
      if (expected == null) {
        expected = BigDecimal.ZERO;
      }

      UUID actualWarehouseUuid =
          b.get("actual_warehouse_id") == null
              ? UUID.fromString(countWarehouseId)
              : scannedUuid("actual_warehouse_id", b.get("actual_warehouse_id"));
      String actualWarehouseId = actualWarehouseUuid.toString();
      // L'emplacement constaté doit appartenir à l'entreprise du comptage : sans ce
      // contrôle, un scan pourrait créditer le solde d'une autre entreprise.
      try (PreparedStatement p =
          c.prepareStatement(
              "SELECT 1 FROM security.warehouse WHERE id = ? AND company_id = ?")) {
        p.setObject(1, actualWarehouseUuid);
        p.setObject(2, UUID.fromString(companyId));
        try (ResultSet r = p.executeQuery()) {
          if (!r.next()) {
            throw new ClientException(
                "Entrepôt constaté invalide",
                "L'entrepôt constaté n'appartient pas à l'entreprise du comptage.",
                "CLIENT_ERROR");
          }
        }
      }
      BigDecimal counted = decimal("counted_quantity", b.get("counted_quantity"), BigDecimal.ONE);
      if (counted.signum() < 0) {
        throw new ClientException(
            "Quantité invalide",
            "La quantité comptée ne peut pas être négative.",
            "CLIENT_ERROR");
      }
      BigDecimal difference = counted.subtract(expected);
      String discrepancy =
          discrepancy(expected, counted, expectedWarehouseId, actualWarehouseId, itemStatus);

      String sql =
          "INSERT INTO security.inventory_count_scan "
              + "(inventory_count_id, item_id, company_id, scanned_by, scanned_at, "
              + "expected_warehouse_id, actual_warehouse_id, expected_quantity, "
              + "counted_quantity, difference, discrepancy_type, notes) "
              + "VALUES (?, ?, ?, ?, COALESCE(?, now()), ?, ?, ?, ?, ?, ?, ?) RETURNING id";
      String generatedId;
      try (PreparedStatement p = c.prepareStatement(sql)) {
        p.setObject(1, countUuid);
        p.setObject(2, itemUuid);
        p.setObject(3, UUID.fromString(companyId));
        p.setObject(4, nullableUuid("scanned_by", b.get("scanned_by")));
        p.setTimestamp(5, nullableTimestamp(b.get("scanned_at")));
        p.setObject(6, expectedWarehouseId == null ? null : UUID.fromString(expectedWarehouseId));
        p.setObject(7, actualWarehouseUuid);
        p.setBigDecimal(8, expected);
        p.setBigDecimal(9, counted);
        p.setBigDecimal(10, difference);
        p.setString(11, discrepancy);
        p.setString(12, b.get("notes") == null ? null : String.valueOf(b.get("notes")));
        try (ResultSet r = p.executeQuery()) {
          if (!r.next()) {
            throw new ServerException("Le scan de comptage n'a pas pu être enregistré.");
          }
          generatedId = r.getString("id");
        }
      }
      return getScanById(generatedId);
    } catch (SQLException e) {
      if ("23505".equals(e.getSQLState())) {
        throw new ClientException(
            "Article déjà scanné",
            "Cet article a déjà été scanné pour ce comptage.",
            "CLIENT_ERROR");
      }
      if ("23503".equals(e.getSQLState())) {
        throw new ClientException(
            "Référence invalide",
            "L'article, l'entrepôt ou le scanneur indiqué est introuvable ou invalide.",
            "CLIENT_ERROR");
      }
      throw new ServerException(e);
    }
  }

  @Override
  public boolean complete(String id, Timestamp completedAt, String completedBy, String notes) {
    String sql =
        "UPDATE security.inventory_count SET status = 'COMPLETED', completed_at = ?, "
            + "completed_by = ?, notes = COALESCE(?, notes) "
            + "WHERE id = ? AND status = 'IN_PROGRESS'";
    try (Connection c = DatabaseConnection.getConnection();
        PreparedStatement p = c.prepareStatement(sql)) {
      p.setTimestamp(1, completedAt);
      p.setObject(2, nullableUuid("completed_by", completedBy));
      p.setString(3, notes == null || notes.isBlank() ? null : notes);
      p.setObject(4, UUID.fromString(id));
      return p.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new ServerException(e);
    }
  }

  @Override
  public boolean cancel(String id) {
    String sql =
        "UPDATE security.inventory_count SET status = 'CANCELLED' "
            + "WHERE id = ? AND status = 'IN_PROGRESS'";
    try (Connection c = DatabaseConnection.getConnection();
        PreparedStatement p = c.prepareStatement(sql)) {
      p.setObject(1, UUID.fromString(id));
      return p.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new ServerException(e);
    }
  }

  private InventoryCountScanResponse getScanById(String scanId) {
    try (Connection c = DatabaseConnection.getConnection();
        PreparedStatement p =
            c.prepareStatement(
                "SELECT s.*, i.qr_code FROM security.inventory_count_scan s "
                    + "JOIN security.item i ON i.id = s.item_id WHERE s.id = ?")) {
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
    if (expectedWarehouseId != null && !expectedWarehouseId.equals(actualWarehouseId)) {
      return "WRONG_LOCATION";
    }
    return "NONE";
  }

  private BigDecimal decimal(String field, Object value, BigDecimal fallback) {
    if (value == null) return fallback;
    try {
      BigDecimal parsed = new BigDecimal(String.valueOf(value));
      if (parsed.scale() > 2) {
        throw new ClientException(
            "Quantité invalide",
            "Le champ '" + field + "' doit avoir au maximum 2 décimales.",
            "CLIENT_ERROR");
      }
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

  @Override
  public List<InventoryCountResponse> list(
      Integer page, Integer size, String search, String where, Object... a) {
    List<InventoryCountResponse> o = new ArrayList<>();
    int p = page == null ? 1 : Math.max(1, page);
    int s = size == null ? 50 : Math.min(MAX_PAGE_SIZE, Math.max(1, size));
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

  @Override
  public long countList(String search, String where, Object... a) {
    String q =
        "SELECT count(*) FROM "
            + inventory_count
            + " WHERE "
            + (where == null ? "1=1" : where)
            + (search == null || search.isBlank()
                ? ""
                : " AND CAST(row_to_json(inventory_count) AS text) ILIKE ?");
    try (Connection c = DatabaseConnection.getConnection();
        PreparedStatement z = c.prepareStatement(q)) {
      int n = 1;
      for (Object x : a) z.setObject(n++, id(x));
      if (search != null && !search.isBlank()) z.setString(n, "%" + search + "%");
      try (ResultSet r = z.executeQuery()) {
        return r.next() ? r.getLong(1) : 0L;
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
  }

  @Override
  public Optional<InventoryCountResponse> one(String v) {
    try (Connection c = DatabaseConnection.getConnection()) {
      return one(c, v);
    } catch (SQLException e) {
      throw new ServerException(e);
    }
  }

  private Optional<InventoryCountResponse> one(Connection c, String v) throws SQLException {
    try (PreparedStatement p =
        c.prepareStatement("SELECT * FROM " + inventory_count + " WHERE id = ?")) {
      p.setObject(1, id(v));
      try (ResultSet r = p.executeQuery()) {
        return r.next() ? Optional.of(mapRowToInventoryCount(r)) : Optional.empty();
      }
    }
  }

  @Override
  public Optional<InventoryCountResponse> one(String v, String companyId) {
    try (Connection c = DatabaseConnection.getConnection();
        PreparedStatement p =
            c.prepareStatement(
                "SELECT * FROM " + inventory_count + " WHERE id = ? AND company_id = ?")) {
      p.setObject(1, id(v));
      p.setObject(2, UUID.fromString(companyId));
      try (ResultSet r = p.executeQuery()) {
        return r.next() ? Optional.of(mapRowToInventoryCount(r)) : Optional.empty();
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
  }
}
