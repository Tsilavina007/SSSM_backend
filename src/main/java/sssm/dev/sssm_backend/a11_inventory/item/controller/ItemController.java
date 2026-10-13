package sssm.dev.sssm_backend.a11_inventory.item.controller;

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

import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemUpdateRequest;
import sssm.dev.sssm_backend.a11_inventory.item.entity.Item;
import sssm.dev.sssm_backend.a11_inventory.item.service.ItemService;

@RestController
@RequestMapping({"/api/items", "/api/inventory/items"})
public class ItemController {
  private final ItemService itemService;

  public ItemController() {
    this.itemService = new ItemService();
  }

  @GetMapping
  public List<Item> getItems(
      @RequestParam Optional<Integer> page,
      @RequestParam Optional<Integer> pageSize,
      @RequestParam Optional<String> search) throws Exception {
    return itemService.getItems(search, page, pageSize);
  }

  @GetMapping("/{itemId}")
  public Optional<Item> getItemById(@PathVariable String itemId) throws Exception {
    return itemService.getItemById(itemId);
  }

  @PostMapping
  public Item createItem(@RequestBody ItemCreateRequest item) throws Exception {
    return itemService.createItem(item);
  }

  @PatchMapping("/{itemId}")
  public Item updateItem(@PathVariable String itemId, @RequestBody ItemUpdateRequest updatedItem) throws Exception {
    return itemService.updateItem(itemId, updatedItem);
  }

  @DeleteMapping("/{itemId}")
  public void deleteItem(@PathVariable String itemId) {
    itemService.deleteItem(itemId);
  }
}
