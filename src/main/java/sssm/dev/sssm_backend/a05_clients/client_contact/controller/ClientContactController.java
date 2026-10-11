package sssm.dev.sssm_backend.a05_clients.client_contact.controller;

import java.util.*;
import org.springframework.web.bind.annotation.*;

import sssm.dev.sssm_backend.a05_clients.client_contact.entity.ClientContact;
import sssm.dev.sssm_backend.a05_clients.client_contact.service.ClientContactService;

@RestController
@RequestMapping("/api/client-contacts")
public class ClientContactController {
    private final ClientContactService service=new ClientContactService();
    @GetMapping("/client/{clientId}") public List<ClientContact> findByClientId(@PathVariable String clientId){return service.findByClientId(clientId);}
    @GetMapping("/{id}") public Optional<ClientContact> findById(@PathVariable String id){return service.findById(id);}
    @PostMapping public ClientContact create(@RequestBody ClientContact contact){return service.create(contact);}
    @PatchMapping("/{id}") public ClientContact update(@PathVariable String id,@RequestBody ClientContact contact){return service.update(id,contact);}
    @DeleteMapping("/{id}") public void delete(@PathVariable String id){service.delete(id);}
}
