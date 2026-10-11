package sssm.dev.sssm_backend.a05_clients.client_contact.repository;

import java.util.List;
import java.util.Optional;
import sssm.dev.sssm_backend.a05_clients.client_contact.entity.ClientContact;

public interface ClientContactDAO {
    List<ClientContact> findByClientId(String clientId);
    Optional<ClientContact> findById(String id);
    ClientContact create(ClientContact contact);
    ClientContact update(ClientContact contact);
    void delete(String id);
}
