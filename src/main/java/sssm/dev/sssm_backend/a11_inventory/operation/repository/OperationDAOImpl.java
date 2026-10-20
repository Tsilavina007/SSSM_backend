package sssm.dev.sssm_backend.a11_inventory.operation.repository;

import java.sql.*;
import java.util.*;
import org.springframework.stereotype.Repository;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;
import sssm.dev.sssm_backend.a01_common.errors.ItemError;
import sssm.dev.sssm_backend.a11_inventory.operation.dto.OperationResponse;

/** JDBC DAO for the inventory operation module. */
@Repository
public class OperationDAOImpl implements OperationDAO {
  private static final String inventory_operation = "security.inventory_operation";

  private Map<String, Object> row(ResultSet r) throws SQLException {
    Map<String, Object> m = new LinkedHashMap<>();
    ResultSetMetaData d = r.getMetaData();
    for (int i = 1; i <= d.getColumnCount(); i++) m.put(d.getColumnLabel(i), r.getObject(i));
    return m;
  }

  private OperationResponse mapRowToOperation(ResultSet r) throws SQLException {
    return new OperationResponse(
        r.getString("id"),
        r.getString("company_id"),
        r.getString("operation_type"),
        r.getString("status"),
        r.getString("reference"),
        r.getString("idempotency_key"),
        r.getString("source_warehouse_id"),
        r.getString("destination_warehouse_id"),
        r.getString("employee_id"),
        r.getString("post_id"),
        r.getString("performed_by"),
        r.getString("notes"),
        r.getTimestamp("created_at"),
        r.getTimestamp("completed_at"));
  }

  private Object id(Object x) {
    try {
      return x == null ? null : UUID.fromString(String.valueOf(x));
    } catch (Exception e) {
      return x;
    }
  }

  public List<Map<String, Object>> list(
      Integer page, Integer size, String search, String where, Object... a) {
    List<Map<String, Object>> o = new ArrayList<>();
    int p = page == null ? 1 : Math.max(1, page),
        s = size == null ? 50 : Math.min(500, Math.max(1, size));
    String q =
        "SELECT * FROM "
            + inventory_operation
            + " WHERE "
            + (where == null ? "1=1" : where)
            + (search == null || search.isBlank()
                ? ""
                : " AND CAST(row_to_json(inventory_operation) AS text) ILIKE ?")
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

  public Optional<OperationResponse> one(String v) {
    try (Connection c = DatabaseConnection.getConnection();
        PreparedStatement p =
            c.prepareStatement("SELECT * FROM " + inventory_operation + " WHERE id=?")) {
      p.setObject(1, id(v));
      try (ResultSet r = p.executeQuery()) {
        return r.next() ? Optional.of(mapRowToOperation(r)) : Optional.empty();
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
  }

  public Object save(String v, Map<String, Object> b, boolean u) {
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
                "Le corps ne contient aucun champ autorisé pour une opération d'inventaire.",
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
              ? "UPDATE " + inventory_operation + " SET " + set + " WHERE id=?"
              : "INSERT INTO " + inventory_operation + " (" + cols + ") VALUES (" + qs + ")";
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
      OperationResponse r = one(v).orElse(null);
      return r != null
          ? r
          : Map.of(
              "errors",
              new ItemError(
                  "Opération d'inventaire introuvable",
                  "Aucune opération d'inventaire ne correspond à l'identifiant " + v + ".",
                  "NOT_FOUND"));
    } catch (SQLException e) {
      throw new ServerException(e);
    }
  }

  public Map<String, Object> delete(String v) {
    try (Connection c = DatabaseConnection.getConnection();
        PreparedStatement p =
            c.prepareStatement("DELETE FROM " + inventory_operation + " WHERE id=?")) {
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
