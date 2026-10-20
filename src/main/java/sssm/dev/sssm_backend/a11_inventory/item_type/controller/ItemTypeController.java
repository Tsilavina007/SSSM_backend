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

import sssm.dev.sssm_backend.a11_inventory.item_type.dto.ItemTypeCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.item_type.dto.ItemTypeUpdateRequest;
import sssm.dev.sssm_backend.a11_inventory.item_type.entity.ItemType;
import sssm.dev.sssm_backend.a11_inventory.item_type.service.ItemTypeService;

@RestController
@RequestMapping("/api/inventory/item-types")
public class ItemTypeController {
  private final ItemTypeService itemTypeService;

  ItemTypeController(ItemTypeService itemTypeService) {
    this.itemTypeService = itemTypeService;
  }

  @GetMapping
  public ResponseEntity<Object> getItemTypes(
      @RequestParam Optional<Integer> page,
      @RequestParam Optional<Integer> pageSize,
      @RequestParam Optional<String> search) {
    List<ItemType> types = itemTypeService.getItemTypes(search, page, pageSize);
    return ResponseEntity.ok().body(types);
  }

  @GetMapping("/{itemTypeId}")
  public ResponseEntity<Object> getItemTypeById(@PathVariable String itemTypeId) {
    Optional<ItemType> types = itemTypeService.getItemTypeById(itemTypeId);
    return ResponseEntity.ok().body(types);
  }

  @PostMapping
  public ResponseEntity<Object> createItemType(@RequestBody ItemTypeCreateRequest itemType) {
    ItemType types = itemTypeService.createItemType(itemType);
    return ResponseEntity.ok().body(types);
  }

  @PatchMapping("/{itemTypeId}")
  public ResponseEntity<Object> updateItemType(
      @PathVariable String itemTypeId, @RequestBody ItemTypeUpdateRequest updatedItemType) {
    ItemType types = itemTypeService.updateItemType(itemTypeId, updatedItemType);
    return ResponseEntity.ok().body(types);
  }

  @DeleteMapping("/{itemTypeId}")
  public ResponseEntity<Object> deleteItemType(@PathVariable String itemTypeId) {
    itemTypeService.deleteItemType(itemTypeId);
    return ResponseEntity.ok().body("ItemType deleted : " + itemTypeId);
  }

  @GetMapping("/category/{itemCategoryId}")
  public ResponseEntity<Object> getItemTypesByItemCategoryId(
      @PathVariable String itemCategoryId,
      @RequestParam Optional<Integer> page,
      @RequestParam Optional<Integer> pageSize) {
    List<ItemType> types =
        itemTypeService.getItemTypesByItemCategoryId(itemCategoryId, page, pageSize);
    return ResponseEntity.ok().body(types);
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
