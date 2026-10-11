package sssm.dev.sssm_backend.personnel.employee.repository;

import java.util.List;
import java.util.Optional;
import sssm.dev.sssm_backend.personnel.employee.entity.Employee;

public interface EmployeeDAO {
    List<Employee> findAll(int page, int pageSize);
    List<Employee> search(String value, int page, int pageSize);
    Optional<Employee> findById(String id);
    Employee create(Employee employee);
    Employee update(Employee employee);
    void delete(String id);
}
