package sssm.dev.sssm_backend.a05_clients.client_contact.service;

import java.util.*;
import sssm.dev.sssm_backend.a05_clients.client_contact.entity.ClientContact;
import sssm.dev.sssm_backend.a05_clients.client_contact.repository.*;

public class ClientContactService {
    private final ClientContactDAO dao=new ClientContactDAOImpl();
    public List<ClientContact> findByClientId(String id){return dao.findByClientId(id);}
    public Optional<ClientContact> findById(String id){return dao.findById(id);}
    public ClientContact create(ClientContact contact){return dao.create(contact);}
    public ClientContact update(String id,ClientContact contact){if(dao.findById(id).isEmpty())throw new IllegalArgumentException("Client contact not found with ID: "+id);contact.setId(id);return dao.update(contact);}
    public void delete(String id){dao.delete(id);}
}
