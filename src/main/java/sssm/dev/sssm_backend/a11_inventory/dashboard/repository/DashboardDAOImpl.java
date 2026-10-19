package sssm.dev.sssm_backend.a11_inventory.dashboard.repository;

import java.sql.*;
import java.util.*;
import org.springframework.stereotype.Repository;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;
import sssm.dev.sssm_backend.a11_inventory.dashboard.dto.DashboardLowStockResponse;
import sssm.dev.sssm_backend.a11_inventory.dashboard.dto.DashboardSummaryResponse;

/** JDBC DAO for the inventory dashboard module. */
@Repository
public class DashboardDAOImpl implements DashboardDAO {
  private static final String item = "security.item";

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

  public List<DashboardSummaryResponse> getSummary() {
    List<DashboardSummaryResponse> o = new ArrayList<>();
    try (Connection c = DatabaseConnection.getConnection();
        PreparedStatement p =
            c.prepareStatement(
                "SELECT (SELECT count(*) FROM security.item) AS items,(SELECT count(*) FROM security.warehouse) AS warehouses,(SELECT count(*) FROM security.inventory_operation) AS operations")) {
      try (ResultSet r = p.executeQuery()) {
        while (r.next())
          o.add(
              new DashboardSummaryResponse(
                  r.getLong("items"), r.getLong("warehouses"), r.getLong("operations")));
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return o;
  }

  public List<DashboardLowStockResponse> getLowStock() {
    List<DashboardLowStockResponse> o = new ArrayList<>();
    try (Connection c = DatabaseConnection.getConnection();
        PreparedStatement p =
            c.prepareStatement(
                "SELECT t.*,count(i.id) AS stock_quantity FROM security.item_type t LEFT JOIN security.item i ON i.item_type_id=t.id GROUP BY t.id HAVING count(i.id)<t.minimum_stock")) {
      try (ResultSet r = p.executeQuery()) {
        while (r.next()) o.add(mapRowToDashboardLowStock(r));
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return o;
  }

  private DashboardLowStockResponse mapRowToDashboardLowStock(ResultSet r) throws SQLException {
    return new DashboardLowStockResponse(
        r.getString("id"),
        r.getString("company_id"),
        r.getString("category_id"),
        r.getString("code"),
        r.getString("name"),
        r.getString("description"),
        r.getString("unit"),
        r.getBigDecimal("quantity_total"),
        r.getBigDecimal("minimum_stock"),
        r.getString("status"),
        r.getTimestamp("created_at"),
        r.getTimestamp("updated_at"),
        r.getTimestamp("archived_at"),
        r.getLong("stock_quantity"));
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
        return r.next() ? row(r) : Map.of("error", "Not found", "id", v);
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
  }

  public Map<String, Object> save(String v, Map<String, Object> b, boolean u) {
    try (Connection c = DatabaseConnection.getConnection()) {
      if (b == null || b.isEmpty()) return Map.of("error", "request body is required");
      List<String> cs = new ArrayList<>();
      List<Object> vs = new ArrayList<>();
      for (var e : b.entrySet())
        if (e.getKey().matches("[A-Za-z_][A-Za-z0-9_]*") && !e.getKey().equals("id")) {
          cs.add(e.getKey());
          vs.add(id(e.getValue()));
        }
      if (cs.isEmpty()) return Map.of("error", "no writable fields");
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
