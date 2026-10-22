package sssm.dev.sssm_backend.a11_inventory.item.controller;

import com.google.zxing.WriterException;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a00_infrastructure.exception.ServerException;
import sssm.dev.sssm_backend.a01_common.errors.ItemError;
import sssm.dev.sssm_backend.a01_common.errors.mapper.ErrorMapper;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemBulkCreateResult;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemCreateRequest;
import sssm.dev.sssm_backend.a11_inventory.item.dto.ItemUpdateRequest;
import sssm.dev.sssm_backend.a11_inventory.item.dto.rest.ItemQrCodeRest;
import sssm.dev.sssm_backend.a11_inventory.item.dto.rest.ItemRest;
import sssm.dev.sssm_backend.a11_inventory.item.service.ItemService;
import sssm.dev.sssm_backend.a11_inventory.item.service.QrCodeService;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.dto.rest.ItemAssignmentRest;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.service.ItemAssignmentService;
import sssm.dev.sssm_backend.a11_inventory.stock_movement.dto.rest.StockMovementRest;
import sssm.dev.sssm_backend.a11_inventory.stock_movement.service.StockMovementService;

@Slf4j
@RestController
@RequestMapping("/api/inventory/items")
public class ItemController {
  private final ItemService itemService;
  private final QrCodeService qrCodeService;
  private final StockMovementService stockMovementService;
  private final ItemAssignmentService itemAssignmentService;
  private final ObjectMapper objectMapper;

  ItemController(
      ItemService itemService,
      QrCodeService qrCodeService,
      StockMovementService stockMovementService,
      ItemAssignmentService itemAssignmentService,
      ObjectMapper objectMapper) {
    this.itemService = itemService;
    this.qrCodeService = qrCodeService;
    this.stockMovementService = stockMovementService;
    this.itemAssignmentService = itemAssignmentService;
    this.objectMapper = objectMapper;
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
    List<StockMovementRest> itemMovements =
        stockMovementService.getStockMovementsByItem(itemId, 1, 500);
    List<ItemAssignmentRest> itemAssignments =
        itemAssignmentService.getItemAssignmentsByItem(itemId, 1, 500);
    Map<String, Object> history = new java.util.LinkedHashMap<>();
    history.put("item_id", itemId);
    history.put("movements", itemMovements);
    history.put("assignments", itemAssignments);
    return history;
  }

  /**
   * Les erreurs sont gérées LOCALEMENT (et non par le GlobalExceptionHandler)
   * car le GlobalExceptionHandler ne peut écrire son corps JSON que si le
   * client l'accepte : avec un Accept: image/png strict, il échoue avec une
   * HttpMediaTypeNotAcceptableException et le corps d'erreur est perdu (cf.
   * "No acceptable representation"). Ici, l'enveloppe ItemError est sérialisée
   * en byte[] avec un Content-Type application/json explicite : la
   * négociation est court-circuitée et l'erreur arrive toujours.
   */
  @GetMapping(value = "/{itemId}/qr-code/image", produces = MediaType.IMAGE_PNG_VALUE)
  public ResponseEntity<byte[]> getQrCodeImage(@PathVariable String itemId) {
    try {
      ItemQrCodeRest qrCodeRest = itemService.getQrCodeByItemId(itemId);
      try {
        byte[] image = qrCodeService.generatePng(qrCodeRest.getQr_code());
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(image);
      } catch (IllegalArgumentException e) {
        // Try imbriqué : cette IllegalArgumentException ne peut venir que du
        // contenu du QR code (ZXing "Found empty contents"), pas de
        // l'analyse de l'itemId.
        log.warn("QR code absent ou invalide pour l'article {} : {}", itemId, e.getMessage());
        throw new ServerException(
            "QR code indisponible",
            "Aucun contenu de QR code valable pour l'article " + itemId + ".",
            "QR_CODE_MISSING");
      } catch (IOException e) {
        log.error("Échec d'écriture de l'image QR code pour l'article {}", itemId, e);
        throw new ServerException(
            "Génération du QR code impossible",
            "Le QR code de l'article " + itemId + " n'a pas pu être généré.",
            "QR_CODE_GENERATION_FAILED");
      } catch (WriterException e) {
        log.error("Échec d'encodage du QR code pour l'article {}", itemId, e);
        throw new ServerException(
            "Génération du QR code impossible",
            "Le QR code de l'article " + itemId + " n'a pas pu être généré.",
            "QR_CODE_GENERATION_FAILED");
      }
    } catch (NotFoundException e) {
      // itemId inconnu : enveloppe ItemError 404.
      return errorResponse(HttpStatus.NOT_FOUND, ErrorMapper.toItemError(e));
    } catch (IllegalArgumentException e) {
      // itemId non UUID (ex. "1") : 400 "Paramètre invalide", et non une
      // erreur QR code trompeuse.
      return errorResponse(HttpStatus.BAD_REQUEST, ErrorMapper.toItemError(e));
    } catch (ServerException e) {
      // Erreurs QR_* ou erreur SQL déjà enveloppée par la couche DAO.
      log.error("Échec de la requête d'image QR code pour l'article {}", itemId, e);
      return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ErrorMapper.toItemError(e));
    }
  }

  /**
   * Construit une réponse d'erreur application/json en byte[] avec un
   * Content-Type explicite pour garantir la livraison de l'enveloppe ItemError
   * même avec un Accept: image/png strict.
   */
  private ResponseEntity<byte[]> errorResponse(HttpStatus status, ItemError error) {
    try {
      return ResponseEntity.status(status)
          .contentType(MediaType.APPLICATION_JSON)
          .body(objectMapper.writeValueAsBytes(Map.of("errors", error)));
    } catch (JacksonException e) {
      // Jackson 3 : JacksonException est runtime, l'échec de sérialisation ne
      // doit jamais masquer le statut HTTP voulu.
      log.error("Échec de sérialisation de l'erreur QR code", e);
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

  @PostMapping("/{itemId}/restore")
  public ItemRest restoreItem(@PathVariable String itemId) {
    return itemService.restoreItem(itemId);
  }

  @DeleteMapping("/{itemId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteItem(@PathVariable String itemId) {
    itemService.deleteItem(itemId);
  }
}
