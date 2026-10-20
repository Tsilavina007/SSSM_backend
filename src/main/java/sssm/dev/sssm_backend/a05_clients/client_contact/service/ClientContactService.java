package sssm.dev.sssm_backend.a05_clients.client_contact.service;

import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a05_clients.client_contact.entity.ClientContact;
import sssm.dev.sssm_backend.a05_clients.client_contact.repository.*;

@Service
@RequiredArgsConstructor
public class ClientContactService {
  private final ClientContactDAO dao;

  public List<ClientContact> findByClientId(String id) {
    return dao.findByClientId(id);
  }

  public Optional<ClientContact> findById(String id) {
    return dao.findById(id);
  }

  public ClientContact create(ClientContact contact) {
    return dao.create(contact);
  }

  public ClientContact update(String id, ClientContact contact) {
    if (dao.findById(id).isEmpty())
      throw new NotFoundException(
          "Contact client introuvable",
          "Aucun contact client ne correspond à l'identifiant " + id + ".",
          "NOT_FOUND");
    contact.setId(id);
    return dao.update(contact);
  }

  public void delete(String id) {
    dao.delete(id);
  }
}
