package sssm.dev.sssm_backend.a11_inventory.item_type.controller;

import java.util.List;
import java.util.Optional;

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
import sssm.dev.sssm_backend.a11_inventory.item_type.dto.ItemTypeCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.item_type.dto.ItemTypeUpdateRequest;
import sssm.dev.sssm_backend.a11_inventory.item_type.entity.ItemType;
import sssm.dev.sssm_backend.a11_inventory.item_type.service.ItemTypeService;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventory/item-types")
public class ItemTypeController {
  private final ItemTypeService itemTypeService;

  @GetMapping
  public ResponseEntity<Object> getItemTypes(
      @RequestParam Optional<Integer> page,
      @RequestParam Optional<Integer> pageSize,
      @RequestParam Optional<String> search) {
    try {
      List<ItemType> types = itemTypeService.getItemTypes(search, page, pageSize);
      return ResponseEntity.ok().body(types);
    } catch (ClientException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    } catch (NotFoundException e) {
      return ResponseEntity.status(NOT_FOUND).body(e.getMessage());
    } catch (ServerException e) {
      return ResponseEntity.internalServerError().body(e.getMessage());
    }
  }

  @GetMapping("/{itemTypeId}")
  public ResponseEntity<Object> getItemTypeById(@PathVariable String itemTypeId) {
    try {
      Optional<ItemType> types = itemTypeService.getItemTypeById(itemTypeId);
      return ResponseEntity.ok().body(types);
    } catch (ClientException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    } catch (NotFoundException e) {
      return ResponseEntity.status(NOT_FOUND).body(e.getMessage());
    } catch (ServerException e) {
      return ResponseEntity.internalServerError().body(e.getMessage());
    }
  }

  @PostMapping
  public ResponseEntity<Object> createItemType(@RequestBody ItemTypeCreateRequest itemType) {
    try {
      ItemType types = itemTypeService.createItemType(itemType);
      return ResponseEntity.ok().body(types);
    } catch (ClientException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    } catch (NotFoundException e) {
      return ResponseEntity.status(NOT_FOUND).body(e.getMessage());
    } catch (ServerException e) {
      return ResponseEntity.internalServerError().body(e.getMessage());
    }
  }

  @PatchMapping("/{itemTypeId}")
  public ResponseEntity<Object> updateItemType(
      @PathVariable String itemTypeId, @RequestBody ItemTypeUpdateRequest updatedItemType) {
    try {
      ItemType types = itemTypeService.updateItemType(itemTypeId, updatedItemType);
      return ResponseEntity.ok().body(types);
    } catch (ClientException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    } catch (NotFoundException e) {
      return ResponseEntity.status(NOT_FOUND).body(e.getMessage());
    } catch (ServerException e) {
      return ResponseEntity.internalServerError().body(e.getMessage());
    }
  }

  @DeleteMapping("/{itemTypeId}")
  public ResponseEntity<Object> deleteItemType(@PathVariable String itemTypeId) {
    try {
      itemTypeService.deleteItemType(itemTypeId);
      return ResponseEntity.ok().body("ItemType deleted : " + itemTypeId);
    } catch (ClientException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    } catch (NotFoundException e) {
      return ResponseEntity.status(NOT_FOUND).body(e.getMessage());
    } catch (ServerException e) {
      return ResponseEntity.internalServerError().body(e.getMessage());
    }
  }

  @GetMapping("/category/{itemCategoryId}")
  public List<ItemType> getItemTypesByItemCategoryId(
      @PathVariable String itemCategoryId,
      @RequestParam Optional<Integer> page,
      @RequestParam Optional<Integer> pageSize)
      throws Exception {
    return itemTypeService.getItemTypesByItemCategoryId(itemCategoryId, page, pageSize);
  }

  @GetMapping("/low-stock")
  public List<ItemType> getItemTypesLowStock() throws Exception {
    return null;
  }

  @GetMapping("/items/{typeId}")
  public List<ItemType> getItemsByItemTypes() throws Exception {
    return null;
  }

  @GetMapping("/stocks/{typeId}")
  public List<ItemType> getStocksByItemTypes() throws Exception {
    return null;
  }
}
