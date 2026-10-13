package sssm.dev.sssm_backend.a11_inventory.item_type.controller;

import java.util.List;
import java.util.Optional;
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
import sssm.dev.sssm_backend.a11_inventory.item_type.dto.ItemTypeCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.item_type.dto.ItemTypeUpdateRequest;
import sssm.dev.sssm_backend.a11_inventory.item_type.entity.ItemType;
import sssm.dev.sssm_backend.a11_inventory.item_type.service.ItemTypeService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventory/item-types")
public class ItemTypeController {
  private final ItemTypeService itemTypeService;

  @GetMapping
  public List<ItemType> getItemTypes(
      @RequestParam Optional<Integer> page,
      @RequestParam Optional<Integer> pageSize,
      @RequestParam Optional<String> search)
      throws Exception {
    return itemTypeService.getItemTypes(search, page, pageSize);
  }

  @GetMapping("/{itemTypeId}")
  public Optional<ItemType> getItemTypeById(@PathVariable String itemTypeId) throws Exception {
    return itemTypeService.getItemTypeById(itemTypeId);
  }

  @PostMapping
  public ItemType createItemType(@RequestBody ItemTypeCreateRequest itemType) throws Exception {
    return itemTypeService.createItemType(itemType);
  }

  @PatchMapping("/{itemTypeId}")
  public ItemType updateItemType(
      @PathVariable String itemTypeId, @RequestBody ItemTypeUpdateRequest updatedItemType)
      throws Exception {
    return itemTypeService.updateItemType(itemTypeId, updatedItemType);
  }

  @DeleteMapping("/{itemTypeId}")
  public void deleteItemType(@PathVariable String itemTypeId) {
    itemTypeService.deleteItemType(itemTypeId);
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
