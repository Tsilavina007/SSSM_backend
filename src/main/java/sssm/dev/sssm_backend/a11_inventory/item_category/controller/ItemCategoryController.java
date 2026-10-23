package sssm.dev.sssm_backend.a11_inventory.item_category.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import sssm.dev.sssm_backend.a11_inventory.item_category.dto.ItemCategoryCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.item_category.dto.ItemCategoryUpdateRequest;
import sssm.dev.sssm_backend.a11_inventory.item_category.dto.rest.ItemCategoryRest;
import sssm.dev.sssm_backend.a11_inventory.item_category.service.ItemCategoryService;

@RestController
@RequestMapping("/api/v1/inventory/item-categories")
public class ItemCategoryController {
  private final ItemCategoryService itemCategoryService;

  ItemCategoryController(ItemCategoryService itemCategoryService) {
    this.itemCategoryService = itemCategoryService;
  }

  @GetMapping
  public List<ItemCategoryRest> getItemCategories(
      @RequestParam Optional<Integer> page,
      @RequestParam Optional<Integer> pageSize,
      @RequestParam Optional<String> search) {
    return itemCategoryService.getItemCategories(search, page, pageSize);
  }

  @GetMapping("/{itemCategoryId}")
  public ItemCategoryRest getItemCategoryById(@PathVariable String itemCategoryId) {
    return itemCategoryService.getItemCategoryById(itemCategoryId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ItemCategoryRest createItemCategory(
      @Valid @RequestBody ItemCategoryCreateRequest itemCategory) {
    return itemCategoryService.createItemCategory(itemCategory);
  }

  @PatchMapping("/{itemCategoryId}")
  public ItemCategoryRest updateItemCategory(
      @PathVariable String itemCategoryId,
      @Valid @RequestBody ItemCategoryUpdateRequest updatedItemCategory) {
    return itemCategoryService.updateItemCategory(itemCategoryId, updatedItemCategory);
  }

  @PostMapping("/{itemCategoryId}/restore")
  public ItemCategoryRest restoreItemCategory(@PathVariable String itemCategoryId) {
    return itemCategoryService.restoreItemCategory(itemCategoryId);
  }

  @DeleteMapping("/{itemCategoryId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteItemCategory(@PathVariable String itemCategoryId) {
    itemCategoryService.deleteItemCategory(itemCategoryId);
  }
}
