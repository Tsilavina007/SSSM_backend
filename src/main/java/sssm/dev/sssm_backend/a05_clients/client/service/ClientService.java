package sssm.dev.sssm_backend.a05_clients.client.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a05_clients.client.dto.ClientCreateRequest;
import sssm.dev.sssm_backend.a05_clients.client.dto.ClientResponse;
import sssm.dev.sssm_backend.a05_clients.client.dto.ClientUpdateRequest;
import sssm.dev.sssm_backend.a05_clients.client.entity.Client;
import sssm.dev.sssm_backend.a05_clients.client.repository.ClientDAO;

@Service
@RequiredArgsConstructor
public class ClientService {
  private final ClientDAO dao;

  public List<ClientResponse> findAll(
      Optional<String> search, Optional<Integer> page, Optional<Integer> size) {
    int p = page.orElse(1), s = size.orElse(10);
    List<Client> clients =
        search.map(value -> dao.search(value, p, s)).orElseGet(() -> dao.findAll(p, s));
    return clients.stream().map(this::toResponse).toList();
  }

  public ClientResponse findById(String id) {
    return dao.findById(id)
        .map(this::toResponse)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Client introuvable",
                    "Aucun client ne correspond à l'identifiant " + id + ".",
                    "NOT_FOUND"));
  }

  public ClientResponse create(ClientCreateRequest request) {
    return toResponse(dao.create(toEntity(request)));
  }

  public ClientResponse update(String id, ClientUpdateRequest request) {
    if (dao.findById(id).isEmpty())
      throw new NotFoundException(
          "Client introuvable",
          "Aucun client ne correspond à l'identifiant " + id + ".",
          "NOT_FOUND");
    Client client = toEntity(request);
    client.setId(id);
    return toResponse(dao.update(client));
  }

  public void delete(String id) {
    dao.delete(id);
  }

  private Client toEntity(ClientCreateRequest request) {
    Client client = new Client();
    client.setCompany_id(request.getCompany_id());
    client.setAgency_id(request.getAgency_id());
    client.setCode(request.getCode());
    client.setName(request.getName());
    client.setLegal_name(request.getLegal_name());
    client.setClient_type(request.getClient_type());
    client.setRegistration_number(request.getRegistration_number());
    client.setTax_number(request.getTax_number());
    client.setEmail(request.getEmail());
    client.setPhone(request.getPhone());
    client.setAddress(request.getAddress());
    client.setCity(request.getCity());
    client.setCountry(request.getCountry());
    client.setPayment_terms_days(
        request.getPayment_terms_days() == null ? 30 : request.getPayment_terms_days());
    client.setActive(request.getActive() == null ? true : request.getActive());
    return client;
  }

  private Client toEntity(ClientUpdateRequest request) {
    Client client = new Client();
    client.setCompany_id(request.getCompany_id());
    client.setAgency_id(request.getAgency_id());
    client.setCode(request.getCode());
    client.setName(request.getName());
    client.setLegal_name(request.getLegal_name());
    client.setClient_type(request.getClient_type());
    client.setRegistration_number(request.getRegistration_number());
    client.setTax_number(request.getTax_number());
    client.setEmail(request.getEmail());
    client.setPhone(request.getPhone());
    client.setAddress(request.getAddress());
    client.setCity(request.getCity());
    client.setCountry(request.getCountry());
    client.setPayment_terms_days(request.getPayment_terms_days());
    client.setActive(request.getActive());
    return client;
  }

  private ClientResponse toResponse(Client client) {
    return new ClientResponse(
        client.getId(),
        client.getCompany_id(),
        client.getAgency_id(),
        client.getCode(),
        client.getName(),
        client.getLegal_name(),
        client.getClient_type(),
        client.getRegistration_number(),
        client.getTax_number(),
        client.getEmail(),
        client.getPhone(),
        client.getAddress(),
        client.getCity(),
        client.getCountry(),
        client.getPayment_terms_days(),
        client.getActive(),
        client.getCreated_at(),
        client.getUpdated_at());
  }
}
