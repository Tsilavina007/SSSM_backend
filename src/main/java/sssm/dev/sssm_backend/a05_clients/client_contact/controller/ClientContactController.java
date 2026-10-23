package sssm.dev.sssm_backend.a05_clients.client_contact.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import sssm.dev.sssm_backend.a05_clients.client_contact.dto.ClientContactCreateRequest;
import sssm.dev.sssm_backend.a05_clients.client_contact.dto.ClientContactResponse;
import sssm.dev.sssm_backend.a05_clients.client_contact.dto.ClientContactUpdateRequest;
import sssm.dev.sssm_backend.a05_clients.client_contact.service.ClientContactService;

@RestController
@RequestMapping("/api/v1/client-contacts")
public class ClientContactController {
  private final ClientContactService service;

  ClientContactController(ClientContactService service) {
    this.service = service;
  }

  @GetMapping("/client/{clientId}")
  public List<ClientContactResponse> findByClientId(@PathVariable String clientId) {
    return service.findByClientId(clientId);
  }

  @GetMapping("/{id}")
  public ClientContactResponse findById(@PathVariable String id) {
    return service.findById(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ClientContactResponse create(@Valid @RequestBody ClientContactCreateRequest request) {
    return service.create(request);
  }

  @PatchMapping("/{id}")
  public ClientContactResponse update(
      @PathVariable String id, @Valid @RequestBody ClientContactUpdateRequest request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable String id) {
    service.delete(id);
  }
}
