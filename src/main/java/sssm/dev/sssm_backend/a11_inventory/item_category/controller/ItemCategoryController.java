package sssm.dev.sssm_backend.a11_inventory.item_category.controller;

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

import sssm.dev.sssm_backend.a11_inventory.item_category.dto.ItemCategoryCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.item_category.dto.ItemCategoryUpdateRequest;
import sssm.dev.sssm_backend.a11_inventory.item_category.entity.ItemCategory;
import sssm.dev.sssm_backend.a11_inventory.item_category.service.ItemCategoryService;

@RestController
@RequestMapping("/api/inventory/item-categories")
public class ItemCategoryController {
  private final ItemCategoryService itemCategoryService;

  ItemCategoryController(ItemCategoryService itemCategoryService) {
    this.itemCategoryService = itemCategoryService;
  }

  @GetMapping
  public ResponseEntity<Object> getItemCategories(
      @RequestParam Optional<Integer> page,
      @RequestParam Optional<Integer> pageSize,
      @RequestParam Optional<String> search) {
    List<ItemCategory> categories = itemCategoryService.getItemCategories(search, page, pageSize);
    return ResponseEntity.ok().body(categories);
  }

  @GetMapping("/{itemCategoryId}")
  public ResponseEntity<Object> getItemCategoryById(@PathVariable String itemCategoryId) {
    Optional<ItemCategory> category = itemCategoryService.getItemCategoryById(itemCategoryId);
    return ResponseEntity.ok().body(category);
  }

  @PostMapping
  public ResponseEntity<Object> createItemCategory(
      @RequestBody ItemCategoryCreateRequest itemCategory) {
    ItemCategory category = itemCategoryService.createItemCategory(itemCategory);
    return ResponseEntity.ok().body(category);
  }

  @PatchMapping("/{itemCategoryId}")
  public ResponseEntity<Object> updateItemCategory(
      @PathVariable String itemCategoryId,
      @RequestBody ItemCategoryUpdateRequest updatedItemCategory) {
    ItemCategory category =
        itemCategoryService.updateItemCategory(itemCategoryId, updatedItemCategory);
    return ResponseEntity.ok().body(category);
  }

  @DeleteMapping("/{itemCategoryId}")
  public ResponseEntity<Object> deleteItemCategory(@PathVariable String itemCategoryId) {
    itemCategoryService.deleteItemCategory(itemCategoryId);
    return ResponseEntity.ok().body("ItemCategory deleted : " + itemCategoryId);
  }
}
