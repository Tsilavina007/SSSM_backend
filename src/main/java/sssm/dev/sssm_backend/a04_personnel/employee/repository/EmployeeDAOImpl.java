package sssm.dev.sssm_backend.a04_personnel.employee.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import sssm.dev.sssm_backend.a00_infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.a01_common.enums.EmployeeStatus;
import sssm.dev.sssm_backend.a04_personnel.employee.entity.Employee;

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
      e.printStackTrace();
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
      e.printStackTrace();
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
      e.printStackTrace();
    }
    return Optional.empty();
  }

  @Override
  public Employee create(Employee employee) {
    String sql =
        "INSERT INTO security.employee (company_id, agency_id, user_id, employee_number, "
            + "first_name, last_name, gender, date_of_birth, phone, email, address, national_id, "
            + "hire_date, end_date, position, status, photo_url, emergency_contact_name, "
            + "emergency_contact_phone, notes) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS security.employee_status), ?, ?, ?, ?)";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      stmt.setObject(1, UUID.fromString(employee.getCompany_id()));
      stmt.setObject(
          2, employee.getAgency_id() == null ? null : UUID.fromString(employee.getAgency_id()));
      stmt.setObject(
          3, employee.getUser_id() == null ? null : UUID.fromString(employee.getUser_id()));
      stmt.setString(4, employee.getEmployee_number());
      stmt.setString(5, employee.getFirst_name());
      stmt.setString(6, employee.getLast_name());
      stmt.setString(7, employee.getGender());
      stmt.setDate(8, employee.getDate_of_birth());
      stmt.setString(9, employee.getPhone());
      stmt.setString(10, employee.getEmail());
      stmt.setString(11, employee.getAddress());
      stmt.setString(12, employee.getNational_id());
      stmt.setDate(13, employee.getHire_date());
      stmt.setDate(14, employee.getEnd_date());
      stmt.setString(15, employee.getPosition());
      stmt.setString(16, employee.getStatus() == null ? null : employee.getStatus().name());
      stmt.setString(17, employee.getPhoto_url());
      stmt.setString(18, employee.getEmergency_contact_name());
      stmt.setString(19, employee.getEmergency_contact_phone());
      stmt.setString(20, employee.getNotes());

      stmt.executeUpdate();

      try (ResultSet rs = stmt.getGeneratedKeys()) {
        if (rs.next()) {
          employee.setId(rs.getString(1));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return employee;
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
      e.printStackTrace();
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
      e.printStackTrace();
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
