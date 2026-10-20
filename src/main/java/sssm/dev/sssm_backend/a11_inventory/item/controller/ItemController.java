package sssm.dev.sssm_backend.a11_inventory.item.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemBulkCreateResult;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemUpdateRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import sssm.dev.sssm_backend.a11_inventory.item.dto.rest.ItemQrCodeRest;
import sssm.dev.sssm_backend.a11_inventory.item.dto.rest.ItemRest;
import sssm.dev.sssm_backend.a11_inventory.item.service.ItemService;
import sssm.dev.sssm_backend.a11_inventory.item.service.QrCodeService;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.dto.rest.ItemAssignmentRest;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.service.ItemAssignmentService;
import sssm.dev.sssm_backend.a11_inventory.stock_movement.dto.rest.StockMovementRest;
import sssm.dev.sssm_backend.a11_inventory.stock_movement.service.StockMovementService;

@RestController
@RequestMapping("/api/inventory/items")
public class ItemController {
  private final ItemService itemService;
  private final QrCodeService qrCodeService;
  private final StockMovementService stockMovementService;
  private final ItemAssignmentService itemAssignmentService;

  ItemController(
      ItemService itemService,
      QrCodeService qrCodeService,
      StockMovementService stockMovementService,
      ItemAssignmentService itemAssignmentService) {
    this.itemService = itemService;
    this.qrCodeService = qrCodeService;
    this.stockMovementService = stockMovementService;
    this.itemAssignmentService = itemAssignmentService;
  }

  @GetMapping
  public List<ItemRest> getItems(
      @RequestParam Optional<Integer> page,
      @RequestParam Optional<Integer> pageSize,
      @RequestParam Optional<String> search,
      @RequestParam Optional<String> warehouseId,
      @RequestParam Optional<String> status) {
    return itemService.getItems(search, page, pageSize, warehouseId, status);
  }

  @GetMapping("/{itemId}")
  public ItemRest getItemById(@PathVariable String itemId) {
    return itemService.getItemById(itemId);
  }

  @GetMapping("/{itemId}/qr-code")
  public ItemQrCodeRest getQrCodeByItemId(@PathVariable String itemId) {
    return itemService.getQrCodeByItemId(itemId);
  }

  @GetMapping("/{itemId}/history")
  public Map<String, Object> getItemHistory(@PathVariable String itemId) {
    List<StockMovementRest> movements = stockMovementService.getStockMovements(Optional.empty(), Optional.of(1), Optional.of(100));
    List<ItemAssignmentRest> assignments = itemAssignmentService.getItemAssignments(Optional.empty(), Optional.of(1), Optional.of(100));
    // Filter by item_id
    List<StockMovementRest> itemMovements = movements.stream()
        .filter(m -> itemId.equals(m.getItem_id()))
        .toList();
    List<ItemAssignmentRest> itemAssignments = assignments.stream()
        .filter(a -> itemId.equals(a.getItem_id()))
        .toList();
    Map<String, Object> history = new java.util.LinkedHashMap<>();
    history.put("item_id", itemId);
    history.put("movements", itemMovements);
    history.put("assignments", itemAssignments);
    return history;
  }

  @GetMapping(value = "/{itemId}/qr-code/image", produces = MediaType.IMAGE_PNG_VALUE)
  public ResponseEntity<byte[]> getQrCodeImage(@PathVariable String itemId) {
    ItemQrCodeRest qrCodeRest = itemService.getQrCodeByItemId(itemId);
    try {
      byte[] image = qrCodeService.generatePng(qrCodeRest.getQr_code());
      return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(image);
    } catch (Exception e) {
      return ResponseEntity.internalServerError().build();
    }
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
