package sssm.dev.sssm_backend.a11_inventory.label.controller;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sssm.dev.sssm_backend.a11_inventory.label.dto.QrLabelBatchRequest;
import sssm.dev.sssm_backend.a11_inventory.label.service.LabelService;

/**
 * Étiquettes imprimables.
 *
 * <p>Les étiquettes sont rendues en HTML : le QR code est intégré dans la page et le texte est
 * lisible, ce qui permet d'imprimer une planche de cent étiquettes en un seul appel.
 */
@RestController
public class LabelController {
  private final LabelService labelService;

  LabelController(LabelService labelService) {
    this.labelService = labelService;
  }

  @GetMapping(value = "/api/v1/inventory/items/{itemId}/label", produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> label(
      @PathVariable String itemId, @RequestParam(required = false) Integer size) {
    return ResponseEntity.ok()
        .contentType(MediaType.TEXT_HTML)
        .body(labelService.renderLabel(itemId, size));
  }

  @PostMapping(value = "/api/v1/inventory/labels/batch", produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> batch(@Valid @RequestBody QrLabelBatchRequest request) {
    return ResponseEntity.ok()
        .contentType(MediaType.TEXT_HTML)
        .body(labelService.renderBatch(request.item_ids(), request.size()));
  }
}
