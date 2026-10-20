package sssm.dev.sssm_backend.a05_clients.client.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import sssm.dev.sssm_backend.a05_clients.client.dto.ClientCreateRequest;
import sssm.dev.sssm_backend.a05_clients.client.dto.ClientResponse;
import sssm.dev.sssm_backend.a05_clients.client.dto.ClientUpdateRequest;
import sssm.dev.sssm_backend.a05_clients.client.service.ClientService;

@RestController
@RequestMapping("/api/clients")
public class ClientController {
  private final ClientService service;

  ClientController(ClientService service) {
    this.service = service;
  }

  @GetMapping
  public List<ClientResponse> findAll(
      @RequestParam Optional<String> search,
      @RequestParam Optional<Integer> page,
      @RequestParam Optional<Integer> pageSize) {
    return service.findAll(search, page, pageSize);
  }

  @GetMapping("/{id}")
  public ClientResponse findById(@PathVariable String id) {
    return service.findById(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ClientResponse create(@Valid @RequestBody ClientCreateRequest request) {
    return service.create(request);
  }

  @PatchMapping("/{id}")
  public ClientResponse update(
      @PathVariable String id, @Valid @RequestBody ClientUpdateRequest request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable String id) {
    service.delete(id);
  }
}
