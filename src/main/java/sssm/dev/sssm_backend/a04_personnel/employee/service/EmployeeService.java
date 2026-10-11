package sssm.dev.sssm_backend.a04_personnel.employee.service;

import java.util.*;
import sssm.dev.sssm_backend.a04_personnel.employee.entity.Employee;
import sssm.dev.sssm_backend.a04_personnel.employee.repository.*;

public class EmployeeService {
    private final EmployeeDAO dao = new EmployeeDAOImpl();
    public List<Employee> findAll(Optional<String> search, Optional<Integer> page, Optional<Integer> size) {
        int p=page.orElse(1), s=size.orElse(10);
        return search.isPresent()?dao.search(search.get(),p,s):dao.findAll(p,s);
    }
    public Optional<Employee> findById(String id){return dao.findById(id);}
    public Employee create(Employee employee){return dao.create(employee);}
    public Employee update(String id,Employee employee){if(dao.findById(id).isEmpty())throw new IllegalArgumentException("Employee not found with ID: "+id);employee.setId(id);return dao.update(employee);}
    public void delete(String id){dao.delete(id);}
}
