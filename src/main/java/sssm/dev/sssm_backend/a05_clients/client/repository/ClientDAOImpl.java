package sssm.dev.sssm_backend.a05_clients.client.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a05_clients.client.entity.Client;

public class ClientDAOImpl implements ClientDAO {

  @Override
  public List<Client> search(String value, int page, int pageSize) {
    String sql =
        "SELECT * FROM security.client WHERE name ILIKE ? OR code ILIKE ? OR legal_name ILIKE ? "
            + "LIMIT ? OFFSET ?";
    List<Client> clients = new ArrayList<>();
    int offset = (page - 1) * pageSize;

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      String searchPattern = "%" + value + "%";
      stmt.setString(1, searchPattern);
      stmt.setString(2, searchPattern);
      stmt.setString(3, searchPattern);
      stmt.setInt(4, pageSize);
      stmt.setInt(5, offset);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          Client client = mapRowToClient(rs);
          clients.add(client);
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return clients;
  }

  @Override
  public List<Client> findAll(int page, int pageSize) {
    String sql = "SELECT * FROM security.client LIMIT ? OFFSET ?";
    List<Client> clients = new ArrayList<>();
    int offset = (page - 1) * pageSize;

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, pageSize);
      stmt.setInt(2, offset);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          Client client = mapRowToClient(rs);
          clients.add(client);
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return clients;
  }

  @Override
  public Optional<Client> findById(String clientId) {
    String sql = "SELECT * FROM security.client WHERE id = ?";
    UUID id = UUID.fromString(clientId);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, id);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapRowToClient(rs));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }

  @Override
  public Client create(Client client) {
    String sql =
        "INSERT INTO security.client (company_id, agency_id, code, name, legal_name, client_type, "
            + "registration_number, tax_number, email, phone, address, city, country, "
            + "payment_terms_days, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      stmt.setObject(1, UUID.fromString(client.getCompany_id()));
      stmt.setObject(
          2, client.getAgency_id() == null ? null : UUID.fromString(client.getAgency_id()));
      stmt.setString(3, client.getCode());
      stmt.setString(4, client.getName());
      stmt.setString(5, client.getLegal_name());
      stmt.setString(6, client.getClient_type());
      stmt.setString(7, client.getRegistration_number());
      stmt.setString(8, client.getTax_number());
      stmt.setString(9, client.getEmail());
      stmt.setString(10, client.getPhone());
      stmt.setString(11, client.getAddress());
      stmt.setString(12, client.getCity());
      stmt.setString(13, client.getCountry());
      stmt.setObject(14, client.getPayment_terms_days());
      stmt.setObject(15, client.getActive());

      stmt.executeUpdate();

      try (ResultSet rs = stmt.getGeneratedKeys()) {
        if (rs.next()) {
          client.setId(rs.getString(1));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return client;
  }

  @Override
  public Client update(Client client) {
    StringBuilder sql = new StringBuilder("UPDATE security.client SET ");
    List<Object> parameters = new ArrayList<>();

    if (client.getCompany_id() != null) {
      sql.append("company_id = ?, ");
      parameters.add(UUID.fromString(client.getCompany_id()));
    }
    if (client.getAgency_id() != null) {
      sql.append("agency_id = ?, ");
      parameters.add(UUID.fromString(client.getAgency_id()));
    }
    if (client.getCode() != null) {
      sql.append("code = ?, ");
      parameters.add(client.getCode());
    }
    if (client.getName() != null) {
      sql.append("name = ?, ");
      parameters.add(client.getName());
    }
    if (client.getLegal_name() != null) {
      sql.append("legal_name = ?, ");
      parameters.add(client.getLegal_name());
    }
    if (client.getClient_type() != null) {
      sql.append("client_type = ?, ");
      parameters.add(client.getClient_type());
    }
    if (client.getRegistration_number() != null) {
      sql.append("registration_number = ?, ");
      parameters.add(client.getRegistration_number());
    }
    if (client.getTax_number() != null) {
      sql.append("tax_number = ?, ");
      parameters.add(client.getTax_number());
    }
    if (client.getEmail() != null) {
      sql.append("email = ?, ");
      parameters.add(client.getEmail());
    }
    if (client.getPhone() != null) {
      sql.append("phone = ?, ");
      parameters.add(client.getPhone());
    }
    if (client.getAddress() != null) {
      sql.append("address = ?, ");
      parameters.add(client.getAddress());
    }
    if (client.getCity() != null) {
      sql.append("city = ?, ");
      parameters.add(client.getCity());
    }
    if (client.getCountry() != null) {
      sql.append("country = ?, ");
      parameters.add(client.getCountry());
    }
    if (client.getPayment_terms_days() != null) {
      sql.append("payment_terms_days = ?, ");
      parameters.add(client.getPayment_terms_days());
    }
    if (client.getActive() != null) {
      sql.append("active = ?, ");
      parameters.add(client.getActive());
    }

    if (parameters.isEmpty()) {
      return client;
    }

    sql.setLength(sql.length() - 2);
    sql.append(" WHERE id = ?");
    parameters.add(UUID.fromString(client.getId()));

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
      for (int index = 0; index < parameters.size(); index++) {
        stmt.setObject(index + 1, parameters.get(index));
      }
      stmt.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return client;
  }

  @Override
  public void delete(String clientId) {
    String sql = "DELETE FROM security.client WHERE id = ?";
    UUID id = UUID.fromString(clientId);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, id);
      stmt.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private Client mapRowToClient(ResultSet rs) throws SQLException {
    return new Client(
        rs.getString("id"),
        rs.getString("company_id"),
        rs.getString("agency_id"),
        rs.getString("code"),
        rs.getString("name"),
        rs.getString("legal_name"),
        rs.getString("client_type"),
        rs.getString("registration_number"),
        rs.getString("tax_number"),
        rs.getString("email"),
        rs.getString("phone"),
        rs.getString("address"),
        rs.getString("city"),
        rs.getString("country"),
        rs.getInt("payment_terms_days"),
        rs.getBoolean("active"),
        rs.getTimestamp("created_at"),
        rs.getTimestamp("updated_at"));
  }
}
