package sssm.dev.sssm_backend.a02_organisation.agency.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;
import sssm.dev.sssm_backend.a02_organisation.agency.entity.Agency;

@Repository
public class AgencyDAOImpl implements AgencyDAO {

  @Override
  public List<Agency> searchAgenciesByName(String searchName, int page, int pageSize) {
    String sql =
        "SELECT * FROM security.agency WHERE name ILIKE ? OR code ILIKE ? LIMIT ? OFFSET ?";
    List<Agency> agencies = new ArrayList<>();
    int offset = (page - 1) * pageSize;

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      String searchPattern = "%" + searchName + "%";
      stmt.setString(1, searchPattern);
      stmt.setString(2, searchPattern);
      stmt.setInt(3, pageSize);
      stmt.setInt(4, offset);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          Agency agency = mapRowToAgency(rs);
          agencies.add(agency);
        }
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return agencies;
  }

  @Override
  public List<Agency> getAllAgencies(int page, int pageSize) {
    String sql = "SELECT * FROM security.agency LIMIT ? OFFSET ?";
    List<Agency> agencies = new ArrayList<>();
    int offset = (page - 1) * pageSize;

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, pageSize);
      stmt.setInt(2, offset);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          Agency agency = mapRowToAgency(rs);
          agencies.add(agency);
        }
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return agencies;
  }

  @Override
  public Optional<Agency> getAgencyById(String agencyId) {
    String sql = "SELECT * FROM security.agency WHERE id = ?";
    UUID id = UUID.fromString(agencyId);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, id);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapRowToAgency(rs));
        }
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return Optional.empty();
  }

  @Override
  public Agency createAgency(Agency agency) {
    String sql =
        "INSERT INTO security.agency (company_id, name, code, address, city, phone, email, "
            + "tax_number, registration_number, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      stmt.setObject(1, UUID.fromString(agency.getCompany_id()));
      stmt.setString(2, agency.getName());
      stmt.setString(3, agency.getCode());
      stmt.setString(4, agency.getAddress());
      stmt.setString(5, agency.getCity());
      stmt.setString(6, agency.getPhone());
      stmt.setString(7, agency.getEmail());
      stmt.setString(8, agency.getTax_number());
      stmt.setString(9, agency.getRegistration_number());
      stmt.setObject(10, agency.getActive());

      stmt.executeUpdate();

      try (ResultSet rs = stmt.getGeneratedKeys()) {
        if (rs.next()) {
          agency.setId(rs.getString(1));
        }
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return agency;
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

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
      for (int index = 0; index < parameters.size(); index++) {
        stmt.setObject(index + 1, parameters.get(index));
      }
      stmt.executeUpdate();
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return agency;
  }

  @Override
  public void deleteAgency(String agencyId) {
    String sql = "DELETE FROM security.agency WHERE id = ?";
    UUID id = UUID.fromString(agencyId);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, id);
      stmt.executeUpdate();
    } catch (SQLException e) {
      throw new ServerException(e);
    }
  }

  private Agency mapRowToAgency(ResultSet rs) throws SQLException {
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
