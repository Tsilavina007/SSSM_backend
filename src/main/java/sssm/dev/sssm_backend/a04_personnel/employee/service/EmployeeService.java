package sssm.dev.sssm_backend.a04_personnel.employee.service;

import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a04_personnel.employee.entity.Employee;
import sssm.dev.sssm_backend.a04_personnel.employee.repository.*;

@Service
@RequiredArgsConstructor
public class EmployeeService {
  private final EmployeeDAO dao;

  public List<Employee> findAll(
      Optional<String> search, Optional<Integer> page, Optional<Integer> size) {
    int p = page.orElse(1), s = size.orElse(10);
    return search.map(value -> dao.search(value, p, s)).orElseGet(() -> dao.findAll(p, s));
  }

  public Optional<Employee> findById(String id) {
    return dao.findById(id);
  }

  public Employee create(Employee employee) {
    return dao.create(employee);
  }

  public Employee update(String id, Employee employee) {
    if (dao.findById(id).isEmpty())
      throw new NotFoundException(
          "Employé introuvable",
          "Aucun employé ne correspond à l'identifiant " + id + ".",
          "NOT_FOUND");
    employee.setId(id);
    return dao.update(employee);
  }

  public void delete(String id) {
    dao.delete(id);
  }
}
