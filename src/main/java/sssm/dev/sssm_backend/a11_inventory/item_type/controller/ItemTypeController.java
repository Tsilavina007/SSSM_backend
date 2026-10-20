package sssm.dev.sssm_backend.a11_inventory.item_type.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import sssm.dev.sssm_backend.a11_inventory.item_type.dto.ItemTypeCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.item_type.dto.ItemTypeUpdateRequest;
import sssm.dev.sssm_backend.a11_inventory.item_type.dto.rest.ItemTypeRest;
import sssm.dev.sssm_backend.a11_inventory.item_type.service.ItemTypeService;

@RestController
@RequestMapping("/api/inventory/item-types")
public class ItemTypeController {
  private final ItemTypeService itemTypeService;

  ItemTypeController(ItemTypeService itemTypeService) {
    this.itemTypeService = itemTypeService;
  }

  @GetMapping
  public List<ItemTypeRest> getItemTypes(
      @RequestParam Optional<Integer> page,
      @RequestParam Optional<Integer> pageSize,
      @RequestParam Optional<String> search) {
    return itemTypeService.getItemTypes(search, page, pageSize);
  }

  @GetMapping("/{itemTypeId}")
  public ItemTypeRest getItemTypeById(@PathVariable String itemTypeId) {
    return itemTypeService.getItemTypeById(itemTypeId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ItemTypeRest createItemType(@Valid @RequestBody ItemTypeCreateRequest itemType) {
    return itemTypeService.createItemType(itemType);
  }

  @PatchMapping("/{itemTypeId}")
  public ItemTypeRest updateItemType(
      @PathVariable String itemTypeId, @Valid @RequestBody ItemTypeUpdateRequest updatedItemType) {
    return itemTypeService.updateItemType(itemTypeId, updatedItemType);
  }

  @DeleteMapping("/{itemTypeId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteItemType(@PathVariable String itemTypeId) {
    itemTypeService.deleteItemType(itemTypeId);
  }

  @GetMapping("/category/{itemCategoryId}")
  public List<ItemTypeRest> getItemTypesByItemCategoryId(
      @PathVariable String itemCategoryId,
      @RequestParam Optional<Integer> page,
      @RequestParam Optional<Integer> pageSize) {
    return itemTypeService.getItemTypesByItemCategoryId(itemCategoryId, page, pageSize);
  }

  @GetMapping("/low-stock")
  public List<ItemTypeRest> getItemTypesLowStock() {
    return List.of();
  }

  @GetMapping("/items/{typeId}")
  public List<ItemTypeRest> getItemsByItemTypes() {
    return List.of();
  }

  @GetMapping("/stocks/{typeId}")
  public List<ItemTypeRest> getStocksByItemTypes() {
    return List.of();
  }
}
