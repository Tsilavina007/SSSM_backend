package sssm.dev.sssm_backend.a02_organisation.agency.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a02_organisation.agency.entity.Agency;

public class AgencyDAOImpl implements AgencyDAO {
  @Override
  public List<Agency> getAllAgencies(int page, int pageSize) {
    return query("SELECT * FROM security.agency LIMIT ? OFFSET ?", null, page, pageSize);
  }

  @Override
  public List<Agency> searchAgenciesByName(String searchName, int page, int pageSize) {
    return query(
        "SELECT * FROM security.agency WHERE name ILIKE ? OR code ILIKE ? LIMIT ? OFFSET ?",
        searchName,
        page,
        pageSize);
  }

  private List<Agency> query(String sql, String searchName, int page, int pageSize) {
    List<Agency> agencies = new ArrayList<>();
    int offset = (page - 1) * pageSize;
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      int index = 1;
      if (searchName != null) {
        stmt.setString(index++, "%" + searchName + "%");
        stmt.setString(index++, "%" + searchName + "%");
      }
      stmt.setInt(index++, pageSize);
      stmt.setInt(index, offset);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) agencies.add(mapRow(rs));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return agencies;
  }

  @Override
  public Optional<Agency> getAgencyById(String agencyId) {
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt =
            conn.prepareStatement("SELECT * FROM security.agency WHERE id = ?")) {
      stmt.setObject(1, UUID.fromString(agencyId));
      try (ResultSet rs = stmt.executeQuery()) {
        return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
      }
    } catch (SQLException e) {
      e.printStackTrace();
      return Optional.empty();
    }
  }

  @Override
  public Agency createAgency(Agency agency) {
    String sql =
        "INSERT INTO security.agency (company_id, name, code, address, city, phone, email, tax_number, registration_number, active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      setValues(stmt, agency, false);
      stmt.executeUpdate();
      try (ResultSet rs = stmt.getGeneratedKeys()) {
        if (rs.next()) agency.setId(rs.getString(1));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return agency;
  }

  private void setValues(PreparedStatement stmt, Agency a, boolean includeId) throws SQLException {
    int i = 1;
    if (includeId) stmt.setObject(i++, UUID.fromString(a.getId()));
    stmt.setObject(i++, UUID.fromString(a.getCompany_id()));
    stmt.setString(i++, a.getName());
    stmt.setString(i++, a.getCode());
    stmt.setString(i++, a.getAddress());
    stmt.setString(i++, a.getCity());
    stmt.setString(i++, a.getPhone());
    stmt.setString(i++, a.getEmail());
    stmt.setString(i++, a.getTax_number());
    stmt.setString(i++, a.getRegistration_number());
    stmt.setObject(i++, a.getActive());
    stmt.setDate(i++, a.getCreated_at());
    stmt.setDate(i, a.getUpdated_at());
  }

  @Override
  public Agency updateAgency(Agency agency) {
    StringBuilder sql = new StringBuilder("UPDATE security.agency SET ");
    List<Object> parameters = new ArrayList<>();

    if (agency.getCompany_id() != null) {
      sql.append("company_id = ?, ");
      parameters.add(UUID.fromString(agency.getCompany_id()));
    }
    if (agency.getName() != null) {
      sql.append("name = ?, ");
      parameters.add(agency.getName());
    }
    if (agency.getCode() != null) {
      sql.append("code = ?, ");
      parameters.add(agency.getCode());
    }
    if (agency.getAddress() != null) {
      sql.append("address = ?, ");
      parameters.add(agency.getAddress());
    }
    if (agency.getCity() != null) {
      sql.append("city = ?, ");
      parameters.add(agency.getCity());
    }
    if (agency.getPhone() != null) {
      sql.append("phone = ?, ");
      parameters.add(agency.getPhone());
    }
    if (agency.getEmail() != null) {
      sql.append("email = ?, ");
      parameters.add(agency.getEmail());
    }
    if (agency.getTax_number() != null) {
      sql.append("tax_number = ?, ");
      parameters.add(agency.getTax_number());
    }
    if (agency.getRegistration_number() != null) {
      sql.append("registration_number = ?, ");
      parameters.add(agency.getRegistration_number());
    }
    if (agency.getActive() != null) {
      sql.append("active = ?, ");
      parameters.add(agency.getActive());
    }

    if (parameters.isEmpty()) {
      return agency;
    }

    sql.setLength(sql.length() - 2);
    sql.append(" WHERE id = ?");
    parameters.add(UUID.fromString(agency.getId()));

    try (Connection connection = DatabaseConnection.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql.toString())) {
      for (int index = 0; index < parameters.size(); index++) {
        statement.setObject(index + 1, parameters.get(index));
      }
      statement.executeUpdate();
    } catch (SQLException exception) {
      exception.printStackTrace();
    }

    return agency;
  }

  @Override
  public void deleteAgency(String agencyId) {
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement("DELETE FROM security.agency WHERE id=?")) {
      stmt.setObject(1, UUID.fromString(agencyId));
      stmt.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private Agency mapRow(ResultSet rs) throws SQLException {
    return new Agency(
        rs.getString("id"),
        rs.getString("company_id"),
        rs.getString("name"),
        rs.getString("code"),
        rs.getString("address"),
        rs.getString("city"),
        rs.getString("phone"),
        rs.getString("email"),
        rs.getString("tax_number"),
        rs.getString("registration_number"),
        rs.getBoolean("active"),
        rs.getDate("created_at"),
        rs.getDate("updated_at"));
  }
}
