package sssm.dev.sssm_backend.a11_inventory.item_category.controller;

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

import sssm.dev.sssm_backend.a11_inventory.item_category.entity.ItemCategory;
import sssm.dev.sssm_backend.a11_inventory.item_category.service.ItemCategoryService;

@RestController
@RequestMapping("/api/item-categories")
public class ItemCategoryController {
  private final ItemCategoryService itemCategoryService;

  public ItemCategoryController() {
    this.itemCategoryService = new ItemCategoryService();
  }

  @GetMapping
  public List<ItemCategory> getItemCategories(
      @RequestParam Optional<Integer> page,
      @RequestParam Optional<Integer> pageSize,
      @RequestParam Optional<String> search) {
    return itemCategoryService.getItemCategories(search, page, pageSize);
  }

  @GetMapping("/{itemCategoryId}")
  public Optional<ItemCategory> getItemCategoryById(@PathVariable String itemCategoryId) {
    return itemCategoryService.getItemCategoryById(itemCategoryId);
  }

  @PostMapping
  public ItemCategory createItemCategory(@RequestBody ItemCategory itemCategory) {
    return itemCategoryService.createItemCategory(itemCategory);
  }

  @PatchMapping("/{itemCategoryId}")
  public ItemCategory updateItemCategory(
      @PathVariable String itemCategoryId, @RequestBody ItemCategory updatedItemCategory) {
    return itemCategoryService.updateItemCategory(itemCategoryId, updatedItemCategory);
  }

  @DeleteMapping("/{itemCategoryId}")
  public void deleteItemCategory(@PathVariable String itemCategoryId) {
    itemCategoryService.deleteItemCategory(itemCategoryId);
  }
}
