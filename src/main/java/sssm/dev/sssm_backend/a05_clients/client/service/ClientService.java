package sssm.dev.sssm_backend.a05_clients.client.service;

import java.util.*;
import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a05_clients.client.entity.Client;
import sssm.dev.sssm_backend.a05_clients.client.repository.*;

public class ClientService {
  private final ClientDAO dao = new ClientDAOImpl();

  public List<Client> findAll(
      Optional<String> search, Optional<Integer> page, Optional<Integer> size) {
    int p = page.orElse(1), s = size.orElse(10);
    return search.isPresent() ? dao.search(search.get(), p, s) : dao.findAll(p, s);
  }

  public Optional<Client> findById(String id) {
    return dao.findById(id);
  }

  public Client create(Client client) {
    return dao.create(client);
  }

  public Client update(String id, Client client) {
    if (dao.findById(id).isEmpty()) throw new NotFoundException("Client introuvable", "Aucun client ne correspond à l'identifiant " + id + ".", "NOT_FOUND");
    client.setId(id);
    return dao.update(client);
  }

  public void delete(String id) {
    dao.delete(id);
  }
}
