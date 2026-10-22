package sssm.dev.sssm_backend.a11_inventory.qr_scan.repository;

import java.sql.*;
import java.util.*;
import org.springframework.stereotype.Repository;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;
import sssm.dev.sssm_backend.a01_common.errors.ItemError;
import sssm.dev.sssm_backend.a11_inventory.qr_scan.dto.QrScanJournalEntry;
import sssm.dev.sssm_backend.a11_inventory.qr_scan.dto.QrScanResolveResponse;

/** JDBC DAO for the inventory qr scan module. */
@Repository
public class QrScanDAOImpl implements QrScanDAO {
  private static final String item = "security.item";

  /**
   * Résolution d'un QR code. Les jointures externes conservent l'article même quand le type, la
   * catégorie ou l'entrepôt sont absents, et l'affectation retenue est celle qui n'est ni retournée
   * ni voidée.
   */
  private static final String RESOLVE_SQL =
      "SELECT i.id, i.company_id, i.qr_code, i.description, i.status, "
          + "t.id AS type_id, t.code AS type_code, t.name AS type_name, "
          + "c.id AS category_id, c.name AS category_name, "
          + "w.id AS warehouse_id, w.name AS warehouse_name, "
          + "a.id AS assignment_id, a.assigned_at, a.expected_return_at, "
          + "e.id AS employee_id, e.employee_number, e.first_name AS employee_first_name, "
          + "e.last_name AS employee_last_name, "
          + "p.id AS post_id, p.code AS post_code, p.name AS post_name "
          + "FROM security.item i "
          + "LEFT JOIN security.item_type t ON t.id = i.item_type_id "
          + "LEFT JOIN security.item_category c ON c.id = i.category_id "
          + "LEFT JOIN security.warehouse w ON w.id = i.current_warehouse_id "
          + "LEFT JOIN security.item_assignment a ON a.item_id = i.id "
          + "AND a.returned_at IS NULL AND a.voided_at IS NULL "
          + "LEFT JOIN security.employee e ON e.id = a.employee_id "
          + "LEFT JOIN security.security_post p ON p.id = a.post_id "
          + "WHERE i.qr_code = ? AND i.company_id = ? AND i.archived_at IS NULL";

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

  private QrScanResolveResponse mapRowToQrScanItem(ResultSet r) throws SQLException {
    QrScanResolveResponse response = new QrScanResolveResponse();
    response.setId(r.getString("id"));
    response.setCompany_id(r.getString("company_id"));
    response.setQr_code(r.getString("qr_code"));
    response.setDescription(r.getString("description"));
    response.setStatus(r.getString("status"));
    response.setType_id(r.getString("type_id"));
    response.setType_code(r.getString("type_code"));
    response.setType_name(r.getString("type_name"));
    response.setCategory_id(r.getString("category_id"));
    response.setCategory_name(r.getString("category_name"));
    response.setWarehouse_id(r.getString("warehouse_id"));
    response.setWarehouse_name(r.getString("warehouse_name"));
    response.setAssignment_id(r.getString("assignment_id"));
    response.setAssigned_at(r.getTimestamp("assigned_at"));
    response.setExpected_return_at(r.getTimestamp("expected_return_at"));
    response.setEmployee_id(r.getString("employee_id"));
    response.setEmployee_number(r.getString("employee_number"));
    response.setEmployee_first_name(r.getString("employee_first_name"));
    response.setEmployee_last_name(r.getString("employee_last_name"));
    response.setPost_id(r.getString("post_id"));
    response.setPost_code(r.getString("post_code"));
    response.setPost_name(r.getString("post_name"));
    return response;
  }

  @Override
  public Optional<QrScanResolveResponse> resolveQrCode(String qrCode, String companyId) {
    try (Connection c = DatabaseConnection.getConnection();
        PreparedStatement p = c.prepareStatement(RESOLVE_SQL)) {
      // Le code est comparé comme texte : un code au format UUID ne doit pas
      // provoquer une erreur de type PostgreSQL.
      p.setString(1, qrCode);
      p.setObject(2, UUID.fromString(companyId));
      try (ResultSet r = p.executeQuery()) {
        if (r.next()) {
          return Optional.of(mapRowToQrScanItem(r));
        }
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return Optional.empty();
  }

  @Override
  public void recordScan(QrScanJournalEntry entry) {
    String sql =
        "INSERT INTO security.qr_scan "
            + "(company_id, item_id, qr_code, scanned_by, device_id, result, scanned_at) "
            + "VALUES (?, ?, ?, ?, ?, CAST(? AS security.qr_scan_result), COALESCE(CAST(? AS timestamptz), now()))";
    try (Connection c = DatabaseConnection.getConnection();
        PreparedStatement p = c.prepareStatement(sql)) {
      p.setObject(1, entry.getCompany_id() == null ? null : UUID.fromString(entry.getCompany_id()));
      p.setObject(2, entry.getItem_id() == null ? null : UUID.fromString(entry.getItem_id()));
      p.setString(3, entry.getQr_code());
      p.setObject(4, entry.getScanned_by() == null ? null : UUID.fromString(entry.getScanned_by()));
      p.setString(5, entry.getDevice_id());
      p.setString(6, entry.getResult());
      if (entry.getScanned_at() == null) {
        p.setNull(7, java.sql.Types.TIMESTAMP_WITH_TIMEZONE);
      } else {
        p.setTimestamp(7, entry.getScanned_at());
      }
      p.executeUpdate();
    } catch (SQLException e) {
      throw new ServerException(e);
    }
  }

  public List<Map<String, Object>> list(
      Integer page, Integer size, String search, String where, Object... a) {
    List<Map<String, Object>> o = new ArrayList<>();
    int p = page == null ? 1 : Math.max(1, page),
        s = size == null ? 50 : Math.min(500, Math.max(1, size));
    String q =
        "SELECT * FROM "
            + item
            + " WHERE "
            + (where == null ? "1=1" : where)
            + (search == null || search.isBlank()
                ? ""
                : " AND CAST(row_to_json(item) AS text) ILIKE ?")
            + " ORDER BY id LIMIT ? OFFSET ?";
    try (Connection c = DatabaseConnection.getConnection();
        PreparedStatement z = c.prepareStatement(q)) {
      int n = 1;
      for (Object x : a) z.setObject(n++, id(x));
      if (search != null && !search.isBlank()) z.setString(n++, "%" + search + "%");
      z.setInt(n++, s);
      z.setInt(n, (p - 1) * s);
      try (ResultSet r = z.executeQuery()) {
        while (r.next()) o.add(row(r));
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return o;
  }

  public Map<String, Object> one(String v) {
    try (Connection c = DatabaseConnection.getConnection();
        PreparedStatement p = c.prepareStatement("SELECT * FROM " + item + " WHERE id=?")) {
      p.setObject(1, id(v));
      try (ResultSet r = p.executeQuery()) {
        return r.next()
            ? row(r)
            : Map.of(
                "errors",
                new ItemError(
                    "Article introuvable",
                    "Aucun article ne correspond à l'identifiant " + v + ".",
                    "NOT_FOUND"));
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
  }

  public Map<String, Object> save(String v, Map<String, Object> b, boolean u) {
    try (Connection c = DatabaseConnection.getConnection()) {
      if (b == null || b.isEmpty())
        return Map.of(
            "errors",
            new ItemError(
                "Corps de requête obligatoire",
                "Le corps JSON de la requête est vide ou absent.",
                "BAD_REQUEST"));
      List<String> cs = new ArrayList<>();
      List<Object> vs = new ArrayList<>();
      for (var e : b.entrySet())
        if (e.getKey().matches("[A-Za-z_][A-Za-z0-9_]*") && !e.getKey().equals("id")) {
          cs.add(e.getKey());
          vs.add(id(e.getValue()));
        }
      if (cs.isEmpty())
        return Map.of(
            "errors",
            new ItemError(
                "Aucun champ modifiable",
                "Le corps ne contient aucun champ autorisé pour un scan QR.",
                "BAD_REQUEST"));
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
              ? "UPDATE " + item + " SET " + set + " WHERE id=?"
              : "INSERT INTO " + item + " (" + cols + ") VALUES (" + qs + ")";
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
      return one(v);
    } catch (SQLException e) {
      throw new ServerException(e);
    }
  }

  public Map<String, Object> delete(String v) {
    try (Connection c = DatabaseConnection.getConnection();
        PreparedStatement p = c.prepareStatement("DELETE FROM " + item + " WHERE id=?")) {
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
