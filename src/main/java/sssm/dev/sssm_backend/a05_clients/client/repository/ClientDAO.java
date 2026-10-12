package sssm.dev.sssm_backend.a05_clients.client.repository;

import java.util.List;
import java.util.Optional;

import sssm.dev.sssm_backend.a05_clients.client.entity.Client;

public interface ClientDAO {
  List<Client> findAll(int page, int pageSize);

  List<Client> search(String value, int page, int pageSize);

  Optional<Client> findById(String id);

  Client create(Client client);

  Client update(Client client);

  void delete(String id);
}
