package sssm.dev.sssm_backend.a04_personnel.employee_document.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a04_personnel.employee_document.dto.EmployeeDocumentCreateRequest;
import sssm.dev.sssm_backend.a04_personnel.employee_document.dto.EmployeeDocumentResponse;
import sssm.dev.sssm_backend.a04_personnel.employee_document.dto.EmployeeDocumentUpdateRequest;
import sssm.dev.sssm_backend.a04_personnel.employee_document.entity.EmployeeDocument;
import sssm.dev.sssm_backend.a04_personnel.employee_document.repository.EmployeeDocumentDAO;

@Service
@RequiredArgsConstructor
public class EmployeeDocumentService {
  private final EmployeeDocumentDAO dao;

  public List<EmployeeDocumentResponse> findByEmployeeId(String employeeId) {
    return dao.findByEmployeeId(employeeId).stream().map(this::toResponse).toList();
  }

  public EmployeeDocumentResponse findById(String id) {
    return dao.findById(id)
        .map(this::toResponse)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Document d'employé introuvable",
                    "Aucun document d'employé ne correspond à l'identifiant " + id + ".",
                    "NOT_FOUND"));
  }

  public EmployeeDocumentResponse create(EmployeeDocumentCreateRequest request) {
    return toResponse(dao.create(toEntity(request)));
  }

  public EmployeeDocumentResponse update(String id, EmployeeDocumentUpdateRequest request) {
    if (dao.findById(id).isEmpty())
      throw new NotFoundException(
          "Document d'employé introuvable",
          "Aucun document d'employé ne correspond à l'identifiant " + id + ".",
          "NOT_FOUND");
    EmployeeDocument document = toEntity(request);
    document.setId(id);
    return toResponse(dao.update(document));
  }

  public void delete(String id) {
    dao.delete(id);
  }

  private EmployeeDocument toEntity(EmployeeDocumentCreateRequest request) {
    EmployeeDocument document = new EmployeeDocument();
    document.setEmployee_id(request.getEmployee_id());
    document.setDocument_type(request.getDocument_type());
    document.setDocument_number(request.getDocument_number());
    document.setIssue_date(request.getIssue_date());
    document.setExpiry_date(request.getExpiry_date());
    document.setFile_url(request.getFile_url());
    document.setNotes(request.getNotes());
    return document;
  }

  private EmployeeDocument toEntity(EmployeeDocumentUpdateRequest request) {
    EmployeeDocument document = new EmployeeDocument();
    document.setEmployee_id(request.getEmployee_id());
    document.setDocument_type(request.getDocument_type());
    document.setDocument_number(request.getDocument_number());
    document.setIssue_date(request.getIssue_date());
    document.setExpiry_date(request.getExpiry_date());
    document.setFile_url(request.getFile_url());
    document.setNotes(request.getNotes());
    return document;
  }

  private EmployeeDocumentResponse toResponse(EmployeeDocument document) {
    return new EmployeeDocumentResponse(
        document.getId(),
        document.getEmployee_id(),
        document.getDocument_type(),
        document.getDocument_number(),
        document.getIssue_date(),
        document.getExpiry_date(),
        document.getFile_url(),
        document.getNotes(),
        document.getCreated_at());
  }
}
