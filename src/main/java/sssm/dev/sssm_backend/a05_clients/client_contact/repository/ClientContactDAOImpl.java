package sssm.dev.sssm_backend.a05_clients.client_contact.repository;

import java.sql.*;
import java.util.*;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a05_clients.client_contact.entity.ClientContact;

public class ClientContactDAOImpl implements ClientContactDAO {
  public List<ClientContact> findByClientId(String clientId) {
    List<ClientContact> out = new ArrayList<>();
    try (Connection c = DatabaseConnection.getConnection();
        PreparedStatement s =
            c.prepareStatement("SELECT * FROM security.client_contact WHERE client_id=?")) {
      s.setObject(1, uuid(clientId));
      try (ResultSet r = s.executeQuery()) {
        while (r.next()) out.add(map(r));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return out;
  }

  public Optional<ClientContact> findById(String id) {
    try (Connection c = DatabaseConnection.getConnection();
        PreparedStatement s =
            c.prepareStatement("SELECT * FROM security.client_contact WHERE id=?")) {
      s.setObject(1, uuid(id));
      try (ResultSet r = s.executeQuery()) {
        return r.next() ? Optional.of(map(r)) : Optional.empty();
      }
    } catch (SQLException e) {
      e.printStackTrace();
      return Optional.empty();
    }
  }

  public ClientContact create(ClientContact x) {
    String q =
        "INSERT INTO security.client_contact(client_id,first_name,last_name,function,email,phone,is_primary) VALUES(?,?,?,?,?,?,?)";
    try (Connection c = DatabaseConnection.getConnection();
        PreparedStatement s = c.prepareStatement(q, Statement.RETURN_GENERATED_KEYS)) {
      values(s, x);
      s.executeUpdate();
      try (ResultSet r = s.getGeneratedKeys()) {
        if (r.next()) x.setId(r.getString(1));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return x;
  }

  public ClientContact update(ClientContact x) {
    String q =
        "UPDATE security.client_contact SET first_name = COALESCE(?, first_name),last_name = COALESCE(?, last_name),function = COALESCE(?, function),email = COALESCE(?, email),phone = COALESCE(?, phone),is_primary = COALESCE(?, is_primary) WHERE id = ?";
    try (Connection c = DatabaseConnection.getConnection();
        PreparedStatement s = c.prepareStatement(q)) {
      s.setString(1, x.getFirst_name());
      s.setString(2, x.getLast_name());
      s.setString(3, x.getFunction());
      s.setString(4, x.getEmail());
      s.setString(5, x.getPhone());
      s.setObject(6, x.getIs_primary());
      s.setObject(7, uuid(x.getId()));
      s.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return x;
  }

  private void values(PreparedStatement s, ClientContact x) throws SQLException {
    s.setObject(1, uuid(x.getClient_id()));
    s.setString(2, x.getFirst_name());
    s.setString(3, x.getLast_name());
    s.setString(4, x.getFunction());
    s.setString(5, x.getEmail());
    s.setString(6, x.getPhone());
    s.setObject(7, x.getIs_primary());
  }

  public void delete(String id) {
    try (Connection c = DatabaseConnection.getConnection();
        PreparedStatement s =
            c.prepareStatement("DELETE FROM security.client_contact WHERE id=?")) {
      s.setObject(1, uuid(id));
      s.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private UUID uuid(String value) {
    return UUID.fromString(value);
  }

  private ClientContact map(ResultSet r) throws SQLException {
    return new ClientContact(
        r.getString("id"),
        r.getString("client_id"),
        r.getString("first_name"),
        r.getString("last_name"),
        r.getString("function"),
        r.getString("email"),
        r.getString("phone"),
        r.getBoolean("is_primary"));
  }
}
