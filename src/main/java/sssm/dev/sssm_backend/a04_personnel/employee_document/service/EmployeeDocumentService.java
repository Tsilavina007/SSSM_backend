package sssm.dev.sssm_backend.a04_personnel.employee_document.service;

import java.util.*;
import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a04_personnel.employee_document.entity.EmployeeDocument;
import sssm.dev.sssm_backend.a04_personnel.employee_document.repository.*;

public class EmployeeDocumentService {
  private final EmployeeDocumentDAO dao = new EmployeeDocumentDAOImpl();

  public List<EmployeeDocument> findByEmployeeId(String employeeId) {
    return dao.findByEmployeeId(employeeId);
  }

  public Optional<EmployeeDocument> findById(String id) {
    return dao.findById(id);
  }

  public EmployeeDocument create(EmployeeDocument document) {
    return dao.create(document);
  }

  public EmployeeDocument update(String id, EmployeeDocument document) {
    if (dao.findById(id).isEmpty())
      throw new NotFoundException("Document d'employé introuvable", "Aucun document d'employé ne correspond à l'identifiant " + id + ".", "NOT_FOUND");
    document.setId(id);
    return dao.update(document);
  }

  public void delete(String id) {
    dao.delete(id);
  }
}
