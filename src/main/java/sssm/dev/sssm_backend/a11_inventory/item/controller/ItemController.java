package sssm.dev.sssm_backend.a11_inventory.item.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemBulkCreateResult;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemUpdateRequest;
import sssm.dev.sssm_backend.a11_inventory.item.dto.rest.ItemQrCodeRest;
import sssm.dev.sssm_backend.a11_inventory.item.dto.rest.ItemRest;
import sssm.dev.sssm_backend.a11_inventory.item.service.ItemService;

@RestController
@RequestMapping("/api/inventory/items")
public class ItemController {
  private final ItemService itemService;

  ItemController(ItemService itemService) {
    this.itemService = itemService;
  }

  @GetMapping
  public List<ItemRest> getItems(
      @RequestParam Optional<Integer> page,
      @RequestParam Optional<Integer> pageSize,
      @RequestParam Optional<String> search) {
    return itemService.getItems(search, page, pageSize);
  }

  @GetMapping("/{itemId}")
  public ItemRest getItemById(@PathVariable String itemId) {
    return itemService.getItemById(itemId);
  }

  @GetMapping("/{itemId}/qr-code")
  public ItemQrCodeRest getQrCodeByItemId(@PathVariable String itemId) {
    return itemService.getQrCodeByItemId(itemId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ItemRest createItem(@Valid @RequestBody ItemCreateRequest item) {
    return itemService.createItem(item);
  }

  @PostMapping("/bulk")
  public ItemBulkCreateResult createItems(@RequestBody List<ItemCreateRequest> items) {
    return itemService.createItems(items);
  }

  @PatchMapping("/{itemId}")
  public ItemRest updateItem(
      @PathVariable String itemId, @Valid @RequestBody ItemUpdateRequest updatedItem) {
    return itemService.updateItem(itemId, updatedItem);
  }

  @DeleteMapping("/{itemId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteItem(@PathVariable String itemId) {
    itemService.deleteItem(itemId);
  }
}
