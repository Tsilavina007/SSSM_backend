package sssm.dev.sssm_backend.a04_personnel.employee.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;
import sssm.dev.sssm_backend.a01_common.enums.EmployeeStatus;
import sssm.dev.sssm_backend.a04_personnel.employee.entity.Employee;

@Repository
public class EmployeeDAOImpl implements EmployeeDAO {

  @Override
  public List<Employee> search(String value, int page, int pageSize) {
    String sql =
        "SELECT * FROM security.employee WHERE first_name ILIKE ? OR last_name ILIKE ? "
            + "OR employee_number ILIKE ? LIMIT ? OFFSET ?";
    List<Employee> employees = new ArrayList<>();
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
          Employee employee = mapRowToEmployee(rs);
          employees.add(employee);
        }
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return employees;
  }

  @Override
  public List<Employee> findAll(int page, int pageSize) {
    String sql = "SELECT * FROM security.employee LIMIT ? OFFSET ?";
    List<Employee> employees = new ArrayList<>();
    int offset = (page - 1) * pageSize;

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, pageSize);
      stmt.setInt(2, offset);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          Employee employee = mapRowToEmployee(rs);
          employees.add(employee);
        }
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return employees;
  }

  @Override
  public Optional<Employee> findById(String employeeId) {
    String sql = "SELECT * FROM security.employee WHERE id = ?";
    UUID id = UUID.fromString(employeeId);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, id);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapRowToEmployee(rs));
        }
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return Optional.empty();
  }

  @Override
  public Employee create(Employee employee) {
    // Les colonnes facultatives ne sont écrites que si elles sont fournies. En particulier,
    // l'absence de status doit laisser PostgreSQL appliquer DEFAULT 'ACTIVE'.
    StringBuilder columns = new StringBuilder("company_id, employee_number, first_name, last_name");
    StringBuilder placeholders = new StringBuilder("?, ?, ?, ?");
    List<Object> parameters = new ArrayList<>();

    parameters.add(UUID.fromString(employee.getCompany_id()));
    parameters.add(employee.getEmployee_number());
    parameters.add(employee.getFirst_name());
    parameters.add(employee.getLast_name());

    if (employee.getAgency_id() != null) {
      columns.append(", agency_id");
      placeholders.append(", ?");
      parameters.add(UUID.fromString(employee.getAgency_id()));
    }
    if (employee.getUser_id() != null) {
      columns.append(", user_id");
      placeholders.append(", ?");
      parameters.add(UUID.fromString(employee.getUser_id()));
    }
    if (employee.getGender() != null) {
      columns.append(", gender");
      placeholders.append(", ?");
      parameters.add(employee.getGender());
    }
    if (employee.getDate_of_birth() != null) {
      columns.append(", date_of_birth");
      placeholders.append(", ?");
      parameters.add(employee.getDate_of_birth());
    }
    if (employee.getPhone() != null) {
      columns.append(", phone");
      placeholders.append(", ?");
      parameters.add(employee.getPhone());
    }
    if (employee.getEmail() != null) {
      columns.append(", email");
      placeholders.append(", ?");
      parameters.add(employee.getEmail());
    }
    if (employee.getAddress() != null) {
      columns.append(", address");
      placeholders.append(", ?");
      parameters.add(employee.getAddress());
    }
    if (employee.getNational_id() != null) {
      columns.append(", national_id");
      placeholders.append(", ?");
      parameters.add(employee.getNational_id());
    }
    if (employee.getHire_date() != null) {
      columns.append(", hire_date");
      placeholders.append(", ?");
      parameters.add(employee.getHire_date());
    }
    if (employee.getEnd_date() != null) {
      columns.append(", end_date");
      placeholders.append(", ?");
      parameters.add(employee.getEnd_date());
    }
    if (employee.getPosition() != null) {
      columns.append(", position");
      placeholders.append(", ?");
      parameters.add(employee.getPosition());
    }
    if (employee.getStatus() != null) {
      columns.append(", status");
      placeholders.append(", CAST(? AS security.employee_status)");
      parameters.add(employee.getStatus().name());
    }
    if (employee.getPhoto_url() != null) {
      columns.append(", photo_url");
      placeholders.append(", ?");
      parameters.add(employee.getPhoto_url());
    }
    if (employee.getEmergency_contact_name() != null) {
      columns.append(", emergency_contact_name");
      placeholders.append(", ?");
      parameters.add(employee.getEmergency_contact_name());
    }
    if (employee.getEmergency_contact_phone() != null) {
      columns.append(", emergency_contact_phone");
      placeholders.append(", ?");
      parameters.add(employee.getEmergency_contact_phone());
    }
    if (employee.getNotes() != null) {
      columns.append(", notes");
      placeholders.append(", ?");
      parameters.add(employee.getNotes());
    }

    String sql =
        "INSERT INTO security.employee ("
            + columns
            + ") VALUES ("
            + placeholders
            + ") RETURNING id";

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      for (int index = 0; index < parameters.size(); index++) {
        stmt.setObject(index + 1, parameters.get(index));
      }

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          String generatedId = rs.getString("id");
          return findById(generatedId)
              .orElseThrow(
                  () -> new ServerException("L'employé créé n'a pas pu être relu depuis la base."));
        }
      }
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    throw new ServerException("La base n'a pas renvoyé l'identifiant de l'employé créé.");
  }

  @Override
  public Employee update(Employee employee) {
    StringBuilder sql = new StringBuilder("UPDATE security.employee SET ");
    List<Object> parameters = new ArrayList<>();

    if (employee.getCompany_id() != null) {
      sql.append("company_id = ?, ");
      parameters.add(UUID.fromString(employee.getCompany_id()));
    }
    if (employee.getAgency_id() != null) {
      sql.append("agency_id = ?, ");
      parameters.add(UUID.fromString(employee.getAgency_id()));
    }
    if (employee.getUser_id() != null) {
      sql.append("user_id = ?, ");
      parameters.add(UUID.fromString(employee.getUser_id()));
    }
    if (employee.getEmployee_number() != null) {
      sql.append("employee_number = ?, ");
      parameters.add(employee.getEmployee_number());
    }
    if (employee.getFirst_name() != null) {
      sql.append("first_name = ?, ");
      parameters.add(employee.getFirst_name());
    }
    if (employee.getLast_name() != null) {
      sql.append("last_name = ?, ");
      parameters.add(employee.getLast_name());
    }
    if (employee.getGender() != null) {
      sql.append("gender = ?, ");
      parameters.add(employee.getGender());
    }
    if (employee.getDate_of_birth() != null) {
      sql.append("date_of_birth = ?, ");
      parameters.add(employee.getDate_of_birth());
    }
    if (employee.getPhone() != null) {
      sql.append("phone = ?, ");
      parameters.add(employee.getPhone());
    }
    if (employee.getEmail() != null) {
      sql.append("email = ?, ");
      parameters.add(employee.getEmail());
    }
    if (employee.getAddress() != null) {
      sql.append("address = ?, ");
      parameters.add(employee.getAddress());
    }
    if (employee.getNational_id() != null) {
      sql.append("national_id = ?, ");
      parameters.add(employee.getNational_id());
    }
    if (employee.getHire_date() != null) {
      sql.append("hire_date = ?, ");
      parameters.add(employee.getHire_date());
    }
    if (employee.getEnd_date() != null) {
      sql.append("end_date = ?, ");
      parameters.add(employee.getEnd_date());
    }
    if (employee.getPosition() != null) {
      sql.append("position = ?, ");
      parameters.add(employee.getPosition());
    }
    if (employee.getStatus() != null) {
      sql.append("status = CAST(? AS security.employee_status), ");
      parameters.add(employee.getStatus().name());
    }
    if (employee.getPhoto_url() != null) {
      sql.append("photo_url = ?, ");
      parameters.add(employee.getPhoto_url());
    }
    if (employee.getEmergency_contact_name() != null) {
      sql.append("emergency_contact_name = ?, ");
      parameters.add(employee.getEmergency_contact_name());
    }
    if (employee.getEmergency_contact_phone() != null) {
      sql.append("emergency_contact_phone = ?, ");
      parameters.add(employee.getEmergency_contact_phone());
    }
    if (employee.getNotes() != null) {
      sql.append("notes = ?, ");
      parameters.add(employee.getNotes());
    }

    if (parameters.isEmpty()) {
      return employee;
    }

    sql.setLength(sql.length() - 2);
    sql.append(" WHERE id = ?");
    parameters.add(UUID.fromString(employee.getId()));

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
      for (int index = 0; index < parameters.size(); index++) {
        stmt.setObject(index + 1, parameters.get(index));
      }
      stmt.executeUpdate();
    } catch (SQLException e) {
      throw new ServerException(e);
    }
    return employee;
  }

  @Override
  public void delete(String employeeId) {
    String sql = "DELETE FROM security.employee WHERE id = ?";
    UUID id = UUID.fromString(employeeId);

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, id);
      stmt.executeUpdate();
    } catch (SQLException e) {
      throw new ServerException(e);
    }
  }

  private Employee mapRowToEmployee(ResultSet rs) throws SQLException {
    return new Employee(
        rs.getString("id"),
        rs.getString("company_id"),
        rs.getString("agency_id"),
        rs.getString("user_id"),
        rs.getString("employee_number"),
        rs.getString("first_name"),
        rs.getString("last_name"),
        rs.getString("gender"),
        rs.getDate("date_of_birth"),
        rs.getString("phone"),
        rs.getString("email"),
        rs.getString("address"),
        rs.getString("national_id"),
        rs.getDate("hire_date"),
        rs.getDate("end_date"),
        rs.getString("position"),
        EmployeeStatus.valueOf(rs.getString("status")),
        rs.getString("photo_url"),
        rs.getString("emergency_contact_name"),
        rs.getString("emergency_contact_phone"),
        rs.getString("notes"),
        rs.getTimestamp("created_at"),
        rs.getTimestamp("updated_at"));
  }
}
