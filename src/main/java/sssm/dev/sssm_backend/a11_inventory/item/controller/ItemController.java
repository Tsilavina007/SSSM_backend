package sssm.dev.sssm_backend.a11_inventory.item.controller;

import java.util.List;
import java.util.Map;
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

import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemUpdateRequest;
import sssm.dev.sssm_backend.a11_inventory.item.dto.rest.ItemQrCodeRest;
import sssm.dev.sssm_backend.a11_inventory.item.entity.Item;
import sssm.dev.sssm_backend.a11_inventory.item.service.ItemService;

@RestController
@RequestMapping("/api/inventory/items")
public class ItemController {
  private final ItemService itemService;

  ItemController(ItemService itemService) {
    this.itemService = itemService;
  }

  @GetMapping
  public ResponseEntity<Object> getItems(
      @RequestParam Optional<Integer> page,
      @RequestParam Optional<Integer> pageSize,
      @RequestParam Optional<String> search) {
    List<Item> items = itemService.getItems(search, page, pageSize);
    return ResponseEntity.ok().body(items);
  }

  @GetMapping("/{itemId}")
  public ResponseEntity<Object> getItemById(@PathVariable String itemId) {
    Optional<Item> item = itemService.getItemById(itemId);
    return ResponseEntity.ok().body(item);
  }

  @GetMapping("/{itemId}/qr-code")
  public ResponseEntity<Object> getQrCodeByItemId(@PathVariable String itemId) {
    Optional<ItemQrCodeRest> item = itemService.getQrCodeByItemId(itemId);
    return ResponseEntity.ok().body(item);
  }

  @PostMapping
  public ResponseEntity<Object> createItem(@RequestBody ItemCreateRequest item) {
    Item itemRes = itemService.createItem(item);
    return ResponseEntity.ok().body(itemRes);
  }

  @SuppressWarnings("unchecked")
  @PostMapping("/bulk")
  public ResponseEntity<Object> createItems(@RequestBody List<ItemCreateRequest> items) {
    List<Object> itemsRes = itemService.createItems(items);
    List<Item> createdItems = (List<Item>) itemsRes.get(0);
    Map<ItemCreateRequest, String> failedItems = (Map<ItemCreateRequest, String>) itemsRes.get(1);
    List<Map<String, Object>> failedItemsWithErrors =
        failedItems.entrySet().stream()
            .map(
                entry ->
                    Map.of(
                        "item", entry.getKey(),
                        "errors", entry.getValue()))
            .toList();
    Map<String, Object> response =
        Map.of(
            "createdItems", createdItems,
            "failedItems", failedItemsWithErrors);
    return ResponseEntity.ok().body(response);
  }

  @PatchMapping("/{itemId}")
  public ResponseEntity<Object> updateItem(
      @PathVariable String itemId, @RequestBody ItemUpdateRequest updatedItem) {
    Item itemRes = itemService.updateItem(itemId, updatedItem);
    return ResponseEntity.ok().body(itemRes);
  }

  @DeleteMapping("/{itemId}")
  public ResponseEntity<Object> deleteItem(@PathVariable String itemId) {
    itemService.deleteItem(itemId);
    return ResponseEntity.ok().body("Item deleted : " + itemId);
  }
}
