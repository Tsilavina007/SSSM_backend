package sssm.dev.sssm_backend.a05_clients.client_contact.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a05_clients.client_contact.entity.ClientContact;

public class ClientContactDAOImpl implements ClientContactDAO {

  @Override
  public List<ClientContact> findByClientId(String clientId) {
    String sql = "SELECT * FROM security.client_contact WHERE client_id = ?";
    List<ClientContact> contacts = new ArrayList<>();
    UUID id = UUID.fromString(clientId);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, id);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          ClientContact contact = mapRowToClientContact(rs);
          contacts.add(contact);
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return contacts;
  }

  @Override
  public Optional<ClientContact> findById(String contactId) {
    String sql = "SELECT * FROM security.client_contact WHERE id = ?";
    UUID id = UUID.fromString(contactId);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, id);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapRowToClientContact(rs));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }

  @Override
  public ClientContact create(ClientContact contact) {
    String sql =
        "INSERT INTO security.client_contact (client_id, first_name, last_name, function, email, "
            + "phone, is_primary) VALUES (?, ?, ?, ?, ?, ?, ?)";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      stmt.setObject(1, UUID.fromString(contact.getClient_id()));
      stmt.setString(2, contact.getFirst_name());
      stmt.setString(3, contact.getLast_name());
      stmt.setString(4, contact.getFunction());
      stmt.setString(5, contact.getEmail());
      stmt.setString(6, contact.getPhone());
      stmt.setObject(7, contact.getIs_primary());

      stmt.executeUpdate();

      try (ResultSet rs = stmt.getGeneratedKeys()) {
        if (rs.next()) {
          contact.setId(rs.getString(1));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return contact;
  }

  @Override
  public ClientContact update(ClientContact contact) {
    StringBuilder sql = new StringBuilder("UPDATE security.client_contact SET ");
    List<Object> parameters = new ArrayList<>();

    if (contact.getClient_id() != null) {
      sql.append("client_id = ?, ");
      parameters.add(UUID.fromString(contact.getClient_id()));
    }
    if (contact.getFirst_name() != null) {
      sql.append("first_name = ?, ");
      parameters.add(contact.getFirst_name());
    }
    if (contact.getLast_name() != null) {
      sql.append("last_name = ?, ");
      parameters.add(contact.getLast_name());
    }
    if (contact.getFunction() != null) {
      sql.append("function = ?, ");
      parameters.add(contact.getFunction());
    }
    if (contact.getEmail() != null) {
      sql.append("email = ?, ");
      parameters.add(contact.getEmail());
    }
    if (contact.getPhone() != null) {
      sql.append("phone = ?, ");
      parameters.add(contact.getPhone());
    }
    if (contact.getIs_primary() != null) {
      sql.append("is_primary = ?, ");
      parameters.add(contact.getIs_primary());
    }

    if (parameters.isEmpty()) {
      return contact;
    }

    sql.setLength(sql.length() - 2);
    sql.append(" WHERE id = ?");
    parameters.add(UUID.fromString(contact.getId()));

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
      for (int index = 0; index < parameters.size(); index++) {
        stmt.setObject(index + 1, parameters.get(index));
      }
      stmt.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return contact;
  }

  @Override
  public void delete(String contactId) {
    String sql = "DELETE FROM security.client_contact WHERE id = ?";
    UUID id = UUID.fromString(contactId);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, id);
      stmt.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private ClientContact mapRowToClientContact(ResultSet rs) throws SQLException {
    return new ClientContact(
        rs.getString("id"),
        rs.getString("client_id"),
        rs.getString("first_name"),
        rs.getString("last_name"),
        rs.getString("function"),
        rs.getString("email"),
        rs.getString("phone"),
        rs.getBoolean("is_primary"));
  }
}
