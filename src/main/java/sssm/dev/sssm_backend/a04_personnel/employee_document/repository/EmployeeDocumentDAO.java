package sssm.dev.sssm_backend.a04_personnel.employee_document.repository;

import java.util.List;
import java.util.Optional;
import sssm.dev.sssm_backend.a04_personnel.employee_document.entity.EmployeeDocument;

public interface EmployeeDocumentDAO {
  List<EmployeeDocument> findByEmployeeId(String employeeId);

  Optional<EmployeeDocument> findById(String id);

  EmployeeDocument create(EmployeeDocument document);

  EmployeeDocument update(EmployeeDocument document);

  void delete(String id);
}
