package sssm.dev.sssm_backend.personnel.employee.repository;

import java.sql.*;
import java.util.*;
import sssm.dev.sssm_backend.infrastructure.database.DatabaseConnection;
import sssm.dev.sssm_backend.personnel.employee.entity.Employee;
import sssm.dev.sssm_backend.common.enums.EmployeeStatus;

public class EmployeeDAOImpl implements EmployeeDAO {
    public List<Employee> findAll(int page, int size) {
        return query("SELECT * FROM security.employee LIMIT ? OFFSET ?", null, page, size);
    }
    public List<Employee> search(String value, int page, int size) {
        return query("SELECT * FROM security.employee WHERE first_name ILIKE ? OR last_name ILIKE ? OR employee_number ILIKE ? LIMIT ? OFFSET ?", value, page, size);
    }
    private List<Employee> query(String sql, String value, int page, int size) {
        List<Employee> result = new ArrayList<>();
        int i = 1;
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            if (value != null) {
                String pattern = "%" + value + "%";
                s.setString(i++, pattern); s.setString(i++, pattern); s.setString(i++, pattern);
            }
            s.setInt(i++, size); s.setInt(i, (page - 1) * size);
            try (ResultSet r = s.executeQuery()) { while (r.next()) result.add(map(r)); }
        } catch (SQLException e) { e.printStackTrace(); }
        return result;
    }
    public Optional<Employee> findById(String id) {
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement("SELECT * FROM security.employee WHERE id=?")) {
            s.setObject(1, uuid(id));
            try (ResultSet r = s.executeQuery()) { return r.next() ? Optional.of(map(r)) : Optional.empty(); }
        } catch (SQLException e) { e.printStackTrace(); return Optional.empty(); }
    }
    public Employee create(Employee e) {
        String sql = "INSERT INTO security.employee (company_id,agency_id,user_id,employee_number,first_name,last_name,gender,date_of_birth,phone,email,address,national_id,hire_date,end_date,position,status,photo_url,emergency_contact_name,emergency_contact_phone,notes) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,CAST(? AS employee_status),?,?,?,?)";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setValues(s, e); s.executeUpdate();
            try (ResultSet r = s.getGeneratedKeys()) { if (r.next()) e.setId(r.getString(1)); }
        } catch (SQLException ex) { ex.printStackTrace(); }
        return e;
    }
    public Employee update(Employee e) {
        String sql = "UPDATE security.employee SET company_id=?,agency_id=?,user_id=?,employee_number=?,first_name=?,last_name=?,gender=?,date_of_birth=?,phone=?,email=?,address=?,national_id=?,hire_date=?,end_date=?,position=?,status=CAST(? AS employee_status),photo_url=?,emergency_contact_name=?,emergency_contact_phone=?,notes=?,updated_at=now() WHERE id=?";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            setValues(s, e); s.setObject(21, uuid(e.getId())); s.executeUpdate();
        } catch (SQLException ex) { ex.printStackTrace(); }
        return e;
    }
    private void setValues(PreparedStatement s, Employee e) throws SQLException {
        s.setObject(1, uuid(e.getCompany_id())); s.setObject(2, uuid(e.getAgency_id())); s.setObject(3, uuid(e.getUser_id()));
        s.setString(4,e.getEmployee_number()); s.setString(5,e.getFirst_name()); s.setString(6,e.getLast_name());
        s.setString(7,e.getGender()); s.setDate(8,e.getDate_of_birth()); s.setString(9,e.getPhone()); s.setString(10,e.getEmail());
        s.setString(11,e.getAddress()); s.setString(12,e.getNational_id()); s.setDate(13,e.getHire_date()); s.setDate(14,e.getEnd_date());
        s.setString(15,e.getPosition()); s.setString(16,e.getStatus() == null ? null : e.getStatus().name()); s.setString(17,e.getPhoto_url());
        s.setString(18,e.getEmergency_contact_name()); s.setString(19,e.getEmergency_contact_phone()); s.setString(20,e.getNotes());
    }
    public void delete(String id) {
        try (Connection c=DatabaseConnection.getConnection(); PreparedStatement s=c.prepareStatement("DELETE FROM security.employee WHERE id=?")) {
            s.setObject(1,uuid(id)); s.executeUpdate();
        } catch(SQLException e){e.printStackTrace();}
    }
    private UUID uuid(String value) { return value == null ? null : UUID.fromString(value); }
    private Employee map(ResultSet r) throws SQLException {
        return new Employee(r.getString("id"),r.getString("company_id"),r.getString("agency_id"),r.getString("user_id"),
            r.getString("employee_number"),r.getString("first_name"),r.getString("last_name"),r.getString("gender"),
            r.getDate("date_of_birth"),r.getString("phone"),r.getString("email"),r.getString("address"),r.getString("national_id"),
            r.getDate("hire_date"),r.getDate("end_date"),r.getString("position"),EmployeeStatus.valueOf(r.getString("status")),r.getString("photo_url"),
            r.getString("emergency_contact_name"),r.getString("emergency_contact_phone"),r.getString("notes"),
            r.getTimestamp("created_at"),r.getTimestamp("updated_at"));
    }
}
