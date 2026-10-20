package sssm.dev.sssm_backend.a05_clients.client_contact.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a05_clients.client_contact.dto.ClientContactCreateRequest;
import sssm.dev.sssm_backend.a05_clients.client_contact.dto.ClientContactResponse;
import sssm.dev.sssm_backend.a05_clients.client_contact.dto.ClientContactUpdateRequest;
import sssm.dev.sssm_backend.a05_clients.client_contact.entity.ClientContact;
import sssm.dev.sssm_backend.a05_clients.client_contact.repository.ClientContactDAO;

@Service
@RequiredArgsConstructor
public class ClientContactService {
  private final ClientContactDAO dao;

  public List<ClientContactResponse> findByClientId(String id) {
    return dao.findByClientId(id).stream().map(this::toResponse).toList();
  }

  public ClientContactResponse findById(String id) {
    return dao.findById(id)
        .map(this::toResponse)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Contact client introuvable",
                    "Aucun contact client ne correspond à l'identifiant " + id + ".",
                    "NOT_FOUND"));
  }

  public ClientContactResponse create(ClientContactCreateRequest request) {
    return toResponse(dao.create(toEntity(request)));
  }

  public ClientContactResponse update(String id, ClientContactUpdateRequest request) {
    if (dao.findById(id).isEmpty())
      throw new NotFoundException(
          "Contact client introuvable",
          "Aucun contact client ne correspond à l'identifiant " + id + ".",
          "NOT_FOUND");
    ClientContact contact = toEntity(request);
    contact.setId(id);
    return toResponse(dao.update(contact));
  }

  public void delete(String id) {
    dao.delete(id);
  }

  private ClientContact toEntity(ClientContactCreateRequest request) {
    ClientContact contact = new ClientContact();
    contact.setClient_id(request.getClient_id());
    contact.setFirst_name(request.getFirst_name());
    contact.setLast_name(request.getLast_name());
    contact.setFunction(request.getFunction());
    contact.setEmail(request.getEmail());
    contact.setPhone(request.getPhone());
    contact.setIs_primary(request.getIs_primary() == null ? false : request.getIs_primary());
    return contact;
  }

  private ClientContact toEntity(ClientContactUpdateRequest request) {
    ClientContact contact = new ClientContact();
    contact.setFirst_name(request.getFirst_name());
    contact.setLast_name(request.getLast_name());
    contact.setFunction(request.getFunction());
    contact.setEmail(request.getEmail());
    contact.setPhone(request.getPhone());
    contact.setIs_primary(request.getIs_primary());
    return contact;
  }

  private ClientContactResponse toResponse(ClientContact contact) {
    return new ClientContactResponse(
        contact.getId(),
        contact.getClient_id(),
        contact.getFirst_name(),
        contact.getLast_name(),
        contact.getFunction(),
        contact.getEmail(),
        contact.getPhone(),
        contact.getIs_primary());
  }
}
