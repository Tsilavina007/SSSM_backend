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

import sssm.dev.sssm_backend.a11_inventory.item_type.entity.ItemType;
import sssm.dev.sssm_backend.a11_inventory.item_type.service.ItemTypeService;

@RestController
@RequestMapping({"/api/item-types", "/api/inventory/types"})
public class ItemTypeController {
  private final ItemTypeService itemTypeService;

  public ItemTypeController() {
    this.itemTypeService = new ItemTypeService();
  }

  @GetMapping
  public List<ItemType> getItemTypes(
      @RequestParam Optional<Integer> page,
      @RequestParam Optional<Integer> pageSize,
      @RequestParam Optional<String> search) {
    return itemTypeService.getItemTypes(search, page, pageSize);
  }

  @GetMapping("/{itemTypeId}")
  public Optional<ItemType> getItemTypeById(@PathVariable String itemTypeId) {
    return itemTypeService.getItemTypeById(itemTypeId);
  }

  @PostMapping
  public ItemType createItemType(@RequestBody ItemType itemType) {
    return itemTypeService.createItemType(itemType);
  }

  @PatchMapping("/{itemTypeId}")
  public ItemType updateItemType(
      @PathVariable String itemTypeId, @RequestBody ItemType updatedItemType) {
    return itemTypeService.updateItemType(itemTypeId, updatedItemType);
  }

  @DeleteMapping("/{itemTypeId}")
  public void deleteItemType(@PathVariable String itemTypeId) {
    itemTypeService.deleteItemType(itemTypeId);
  }
}
