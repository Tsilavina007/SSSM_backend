package sssm.dev.sssm_backend.a11_inventory.item.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ClientException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemUpdateRequest;
import sssm.dev.sssm_backend.a11_inventory.item.entity.Item;
import sssm.dev.sssm_backend.a11_inventory.item.service.ItemService;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventory/items")
public class ItemController {
  @Autowired 
  private ItemService itemService;

  @GetMapping
  public ResponseEntity<Object> getItems(
      @RequestParam Optional<Integer> page,
      @RequestParam Optional<Integer> pageSize,
      @RequestParam Optional<String> search) {
    try {
      List<Item> items = itemService.getItems(search, page, pageSize);
      return ResponseEntity.ok().body(items);
    } catch (ClientException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    } catch (NotFoundException e) {
      return ResponseEntity.status(NOT_FOUND).body(e.getMessage());
    } catch (ServerException e) {
      return ResponseEntity.internalServerError().body(e.getMessage());
    }
  }

  @GetMapping("/{itemId}")
  public ResponseEntity<Object> getItemById(@PathVariable String itemId) {
    try {
      Optional<Item> item = itemService.getItemById(itemId);
      return ResponseEntity.ok().body(item);
    } catch (ClientException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    } catch (NotFoundException e) {
      return ResponseEntity.status(NOT_FOUND).body(e.getMessage());
    } catch (ServerException e) {
      return ResponseEntity.internalServerError().body(e.getMessage());
    }
  }

  @PostMapping
  public ResponseEntity<Object> createItem(@RequestBody ItemCreateRequest item) throws Exception {
    try {
      Item itemRes = itemService.createItem(item);
      return ResponseEntity.ok().body(itemRes);
    } catch (ClientException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    } catch (NotFoundException e) {
      return ResponseEntity.status(NOT_FOUND).body(e.getMessage());
    } catch (ServerException e) {
      return ResponseEntity.internalServerError().body(e.getMessage());
    }
  }

  @PatchMapping("/{itemId}")
  public ResponseEntity<Object> updateItem(@PathVariable String itemId, @RequestBody ItemUpdateRequest updatedItem) {
    try {
      Item itemRes = itemService.updateItem(itemId, updatedItem);
      return ResponseEntity.ok().body(itemRes);
    } catch (ClientException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    } catch (NotFoundException e) {
      return ResponseEntity.status(NOT_FOUND).body(e.getMessage());
    } catch (ServerException e) {
      return ResponseEntity.internalServerError().body(e.getMessage());
    }
  }

  @DeleteMapping("/{itemId}")
  public ResponseEntity<Object> deleteItem(@PathVariable String itemId) {
    try {
      itemService.deleteItem(itemId);
    return ResponseEntity.ok().body("Item deleted : " + itemId);
    } catch (ClientException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    } catch (NotFoundException e) {
      return ResponseEntity.status(NOT_FOUND).body(e.getMessage());
    } catch (ServerException e) {
      return ResponseEntity.internalServerError().body(e.getMessage());
    }
  }
}
