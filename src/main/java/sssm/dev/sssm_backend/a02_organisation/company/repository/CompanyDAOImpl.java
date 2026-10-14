package sssm.dev.sssm_backend.a02_organisation.company.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;
import sssm.dev.sssm_backend.a02_organisation.company.entity.Company;

@Repository 
public class CompanyDAOImpl implements CompanyDAO {

  @Override
  public List<Company> searchCompaniesByName(String searchName, int page, int pageSize) {
    String sql =
        "SELECT c.* FROM security.company c WHERE name ILIKE '%"
            + searchName
            + "%' OR legal_name ILIKE '%"
            + searchName
            + "%' LIMIT ? OFFSET ?";
    List<Company> companies = new ArrayList<>();
    int offset = (page - 1) * pageSize;

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setInt(1, pageSize);
      stmt.setInt(2, offset);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          Company company = mapRowToCompany(rs);
          companies.add(company);
        }
      }
    } catch (SQLException e) {
      throw new ServerException("Error when searchCompaniesByName : " + e);
    }
    return companies;
  }

  @Override
  public List<Company> getAllCompanies(int page, int pageSize) {
    String sql = "SELECT * FROM security.company LIMIT ? OFFSET ?";
    List<Company> companies = new ArrayList<>();
    int offset = (page - 1) * pageSize;

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setInt(1, pageSize);
      stmt.setInt(2, offset);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          Company company = mapRowToCompany(rs);
          companies.add(company);
        }
      }
    } catch (SQLException e) {
      throw new ServerException("Error when getAllCompanies : " + e);
    }
    return companies;
  }

  @Override
  public Optional<Company> getCompanyById(String companyId) {
    String sql = "SELECT * FROM security.company WHERE id = ?";
    UUID id = UUID.fromString(companyId);
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setObject(1, id);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapRowToCompany(rs));
        }
        throw new NotFoundException("Company with id : " + companyId + " not Found");
      }
    } catch (SQLException e) {
      throw new ServerException("Error when getCompanyById : " + e);
    }
  }

  @Override
  public Company createCompany(Company company) {
    String sql =
        "INSERT INTO security.company (name, legal_name, registration_number, tax_number, email, phone, address, city, country, logo_url, currency_code) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

      stmt.setString(1, company.getName());
      stmt.setString(2, company.getLegal_name());
      stmt.setString(3, company.getRegistration_number());
      stmt.setString(4, company.getTax_number());
      stmt.setString(5, company.getEmail());
      stmt.setString(6, company.getPhone());
      stmt.setString(7, company.getAddress());
      stmt.setString(8, company.getCity());
      stmt.setString(9, company.getCountry());
      stmt.setString(10, company.getLogo_url());
      stmt.setString(11, company.getCurrency_code());

      stmt.executeUpdate();

      try (ResultSet rs = stmt.getGeneratedKeys()) {
        if (rs.next()) {
          return mapRowToCompany(rs);
        }
      }
    } catch (SQLException e) {
      throw new ServerException("Error when createCompany : " + e);
    }
    return null;
  }

  @Override
  public Company updateCompany(Company company) {
    StringBuilder sql = new StringBuilder("UPDATE security.company SET ");
    List<Object> parameters = new ArrayList<>();

    if (company.getName() != null) {
      sql.append("name = ?, ");
      parameters.add(company.getName());
    }
    if (company.getLegal_name() != null) {
      sql.append("legal_name = ?, ");
      parameters.add(company.getLegal_name());
    }
    if (company.getRegistration_number() != null) {
      sql.append("registration_number = ?, ");
      parameters.add(company.getRegistration_number());
    }
    if (company.getTax_number() != null) {
      sql.append("tax_number = ?, ");
      parameters.add(company.getTax_number());
    }
    if (company.getEmail() != null) {
      sql.append("email = ?, ");
      parameters.add(company.getEmail());
    }
    if (company.getPhone() != null) {
      sql.append("phone = ?, ");
      parameters.add(company.getPhone());
    }
    if (company.getAddress() != null) {
      sql.append("address = ?, ");
      parameters.add(company.getAddress());
    }
    if (company.getCity() != null) {
      sql.append("city = ?, ");
      parameters.add(company.getCity());
    }
    if (company.getCountry() != null) {
      sql.append("country = ?, ");
      parameters.add(company.getCountry());
    }
    if (company.getLogo_url() != null) {
      sql.append("logo_url = ?, ");
      parameters.add(company.getLogo_url());
    }
    if (company.getCurrency_code() != null) {
      sql.append("currency_code = ?, ");
      parameters.add(company.getCurrency_code());
    }

    if (parameters.isEmpty()) {
      return company;
    }

    sql.setLength(sql.length() - 2);
    sql.append(" WHERE id = ?");
    parameters.add(UUID.fromString(company.getId()));

    try (Connection connection = DatabaseConnection.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql.toString())) {
      for (int index = 0; index < parameters.size(); index++) {
        statement.setObject(index + 1, parameters.get(index));
      }
      statement.executeUpdate();
    } catch (SQLException e) {
      throw new ServerException("Error when updateCompany : " + e);
    }
    return company;
  }

  @Override
  public void deleteCompany(String companyId) {
    String sql = "DELETE FROM security.company WHERE id = ?";
    UUID id = UUID.fromString(companyId);
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setObject(1, id);
      stmt.executeUpdate();
    } catch (SQLException e) {
      throw new ServerException("Error when deleteCompany : " + e);
    }
  }

  private Company mapRowToCompany(ResultSet rs) throws SQLException {
    return new Company(
        rs.getString("id"),
        rs.getString("name"),
        rs.getString("legal_name"),
        rs.getString("registration_number"),
        rs.getString("tax_number"),
        rs.getString("email"),
        rs.getString("phone"),
        rs.getString("address"),
        rs.getString("city"),
        rs.getString("country"),
        rs.getString("logo_url"),
        rs.getString("currency_code"),
        rs.getDate("created_at"),
        rs.getDate("updated_at"));
  }
}
