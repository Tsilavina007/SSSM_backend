package sssm.dev.sssm_backend.a04_personnel.employee.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a04_personnel.employee.dto.EmployeeCreateRequest;
import sssm.dev.sssm_backend.a04_personnel.employee.dto.EmployeeResponse;
import sssm.dev.sssm_backend.a04_personnel.employee.dto.EmployeeUpdateRequest;
import sssm.dev.sssm_backend.a04_personnel.employee.entity.Employee;
import sssm.dev.sssm_backend.a04_personnel.employee.repository.EmployeeDAO;

@Service
@RequiredArgsConstructor
public class EmployeeService {
  private final EmployeeDAO dao;

  public List<EmployeeResponse> findAll(
      Optional<String> search, Optional<Integer> page, Optional<Integer> size) {
    int p = page.orElse(1), s = size.orElse(10);
    List<Employee> employees =
        search.map(value -> dao.search(value, p, s)).orElseGet(() -> dao.findAll(p, s));
    return employees.stream().map(this::toResponse).toList();
  }

  public EmployeeResponse findById(String id) {
    return dao.findById(id)
        .map(this::toResponse)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Employé introuvable",
                    "Aucun employé ne correspond à l'identifiant " + id + ".",
                    "NOT_FOUND"));
  }

  public EmployeeResponse create(EmployeeCreateRequest request) {
    return toResponse(dao.create(toEntity(request)));
  }

  public EmployeeResponse update(String id, EmployeeUpdateRequest request) {
    if (dao.findById(id).isEmpty())
      throw new NotFoundException(
          "Employé introuvable",
          "Aucun employé ne correspond à l'identifiant " + id + ".",
          "NOT_FOUND");
    Employee employee = toEntity(request);
    employee.setId(id);
    return toResponse(dao.update(employee));
  }

  public void delete(String id) {
    dao.delete(id);
  }

  private Employee toEntity(EmployeeCreateRequest request) {
    Employee employee = new Employee();
    employee.setCompany_id(request.getCompany_id());
    employee.setAgency_id(request.getAgency_id());
    employee.setUser_id(request.getUser_id());
    employee.setEmployee_number(request.getEmployee_number());
    employee.setFirst_name(request.getFirst_name());
    employee.setLast_name(request.getLast_name());
    employee.setGender(request.getGender());
    employee.setDate_of_birth(request.getDate_of_birth());
    employee.setPhone(request.getPhone());
    employee.setEmail(request.getEmail());
    employee.setAddress(request.getAddress());
    employee.setNational_id(request.getNational_id());
    employee.setHire_date(request.getHire_date());
    employee.setEnd_date(request.getEnd_date());
    employee.setPosition(request.getPosition());
    employee.setStatus(request.getStatus());
    employee.setPhoto_url(request.getPhoto_url());
    employee.setEmergency_contact_name(request.getEmergency_contact_name());
    employee.setEmergency_contact_phone(request.getEmergency_contact_phone());
    employee.setNotes(request.getNotes());
    return employee;
  }

  private Employee toEntity(EmployeeUpdateRequest request) {
    Employee employee = new Employee();
    employee.setCompany_id(request.getCompany_id());
    employee.setAgency_id(request.getAgency_id());
    employee.setUser_id(request.getUser_id());
    employee.setEmployee_number(request.getEmployee_number());
    employee.setFirst_name(request.getFirst_name());
    employee.setLast_name(request.getLast_name());
    employee.setGender(request.getGender());
    employee.setDate_of_birth(request.getDate_of_birth());
    employee.setPhone(request.getPhone());
    employee.setEmail(request.getEmail());
    employee.setAddress(request.getAddress());
    employee.setNational_id(request.getNational_id());
    employee.setHire_date(request.getHire_date());
    employee.setEnd_date(request.getEnd_date());
    employee.setPosition(request.getPosition());
    employee.setStatus(request.getStatus());
    employee.setPhoto_url(request.getPhoto_url());
    employee.setEmergency_contact_name(request.getEmergency_contact_name());
    employee.setEmergency_contact_phone(request.getEmergency_contact_phone());
    employee.setNotes(request.getNotes());
    return employee;
  }

  private EmployeeResponse toResponse(Employee employee) {
    return new EmployeeResponse(
        employee.getId(),
        employee.getCompany_id(),
        employee.getAgency_id(),
        employee.getUser_id(),
        employee.getEmployee_number(),
        employee.getFirst_name(),
        employee.getLast_name(),
        employee.getGender(),
        employee.getDate_of_birth(),
        employee.getPhone(),
        employee.getEmail(),
        employee.getAddress(),
        employee.getNational_id(),
        employee.getHire_date(),
        employee.getEnd_date(),
        employee.getPosition(),
        employee.getStatus(),
        employee.getPhoto_url(),
        employee.getEmergency_contact_name(),
        employee.getEmergency_contact_phone(),
        employee.getNotes(),
        employee.getCreated_at(),
        employee.getUpdated_at());
  }
}
