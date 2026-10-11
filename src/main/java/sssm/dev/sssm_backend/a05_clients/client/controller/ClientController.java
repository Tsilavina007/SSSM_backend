package sssm.dev.sssm_backend.a05_clients.client.controller;

import java.util.*;
import org.springframework.web.bind.annotation.*;

import sssm.dev.sssm_backend.a05_clients.client.entity.Client;
import sssm.dev.sssm_backend.a05_clients.client.service.ClientService;

@RestController
@RequestMapping("/api/clients")
public class ClientController {
    private final ClientService service=new ClientService();
    @GetMapping public List<Client> findAll(@RequestParam Optional<String> search,@RequestParam Optional<Integer> page,@RequestParam Optional<Integer> pageSize){return service.findAll(search,page,pageSize);}
    @GetMapping("/{id}") public Optional<Client> findById(@PathVariable String id){return service.findById(id);}
    @PostMapping public Client create(@RequestBody Client client){return service.create(client);}
    @PatchMapping("/{id}") public Client update(@PathVariable String id,@RequestBody Client client){return service.update(id,client);}
    @DeleteMapping("/{id}") public void delete(@PathVariable String id){service.delete(id);}
}
