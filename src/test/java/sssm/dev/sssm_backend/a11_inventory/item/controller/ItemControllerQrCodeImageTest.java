package sssm.dev.sssm_backend.a11_inventory.item.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.zxing.WriterException;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a01_common.errors.handler.GlobalExceptionHandler;
import sssm.dev.sssm_backend.a11_inventory.item.dto.rest.ItemQrCodeRest;
import sssm.dev.sssm_backend.a11_inventory.item.service.ItemService;
import sssm.dev.sssm_backend.a11_inventory.item.service.QrCodeService;
import sssm.dev.sssm_backend.a11_inventory.item_assignment.service.ItemAssignmentService;
import sssm.dev.sssm_backend.a11_inventory.stock_movement.service.StockMovementService;

/**
 * Tests du endpoint d'image QR code ({@code GET /{itemId}/qr-code/image}) :
 * succès en image/png et enveloppe d'erreurs ItemError (404 article
 * introuvable, 500 QR_CODE_MISSING, 500 QR_CODE_GENERATION_FAILED).
 */
class ItemControllerQrCodeImageTest {

  private static final String BASE_URL = "/api/inventory/items";
  private static final String ITEM_ID = "56a2a24b-602d-427a-9cbc-9965df5a5ecd";
  private static final String QR_CONTENT = "SCAN-123";

  private ItemService itemService;
  private QrCodeService qrCodeService;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    itemService = mock(ItemService.class);
    qrCodeService = mock(QrCodeService.class);
    ItemController controller =
        new ItemController(
            itemService,
            qrCodeService,
            mock(StockMovementService.class),
            mock(ItemAssignmentService.class),
            new ObjectMapper());
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  private static ItemQrCodeRest qrCodeRest(String qrCode) {
    return new ItemQrCodeRest(
        ITEM_ID,
        "b0000000-0000-0000-0000-000000000001",
        null,
        "t0000000-0000-0000-0000-000000000001",
        "w0000000-0000-0000-0000-000000000001",
        qrCode);
  }

  @Test
  void returnsPngImageForKnownItem() throws Exception {
    byte[] png = new QrCodeService().generatePng(QR_CONTENT);
    when(itemService.getQrCodeByItemId(ITEM_ID)).thenReturn(qrCodeRest(QR_CONTENT));
    when(qrCodeService.generatePng(QR_CONTENT)).thenReturn(png);

    mockMvc
        .perform(get(BASE_URL + "/" + ITEM_ID + "/qr-code/image"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_PNG))
        .andExpect(content().bytes(png));
  }

  @Test
  void returnsNotFoundEnvelopeForUnknownItem() throws Exception {
    when(itemService.getQrCodeByItemId(ITEM_ID))
        .thenThrow(
            new NotFoundException(
                "Article introuvable",
                "Aucun article ne correspond à l'identifiant " + ITEM_ID + ".",
                "NOT_FOUND"));

    mockMvc
        .perform(get(BASE_URL + "/" + ITEM_ID + "/qr-code/image"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errors.code").value("NOT_FOUND"))
        .andExpect(jsonPath("$.errors.message").value("Article introuvable"))
        .andExpect(jsonPath("$.errors.detail").exists());
  }

  @Test
  void returnsBadRequestForInvalidItemId() throws Exception {
    // Simule ItemDAOImpl.getItemById : UUID.fromString("1") hors try →
    // IllegalArgumentException("Invalid UUID string: 1") qui doit remonter
    // au GlobalExceptionHandler (400) et non être capturée en erreur QR code.
    when(itemService.getQrCodeByItemId("1"))
        .thenThrow(new IllegalArgumentException("Invalid UUID string: 1"));

    mockMvc
        .perform(get(BASE_URL + "/1/qr-code/image"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.code").value("BAD_REQUEST"))
        .andExpect(jsonPath("$.errors.message").value("Paramètre invalide"))
        .andExpect(jsonPath("$.errors.detail").value("La valeur '1' n'est pas un UUID valide."));
  }

  @Test
  void deliversJsonErrorBodyEvenWithStrictImagePngAccept() throws Exception {
    // Reproduit l'incident observé en production : avec un Accept: image/png
    // strict, le GlobalExceptionHandler échouait en écrivant l'enveloppe JSON
    // (HttpMediaTypeNotAcceptableException "No acceptable representation") et
    // le corps d'erreur était perdu. La gestion locale doit livrer l'erreur.
    when(itemService.getQrCodeByItemId(ITEM_ID))
        .thenThrow(
            new NotFoundException(
                "Article introuvable",
                "Aucun article ne correspond à l'identifiant " + ITEM_ID + ".",
                "NOT_FOUND"));

    mockMvc
        .perform(get(BASE_URL + "/" + ITEM_ID + "/qr-code/image").accept(MediaType.IMAGE_PNG))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.errors.code").value("NOT_FOUND"))
        .andExpect(jsonPath("$.errors.message").value("Article introuvable"));
  }

  @Test
  void returnsQrCodeMissingWhenQrCodeContentIsEmpty() throws Exception {
    when(itemService.getQrCodeByItemId(ITEM_ID)).thenReturn(qrCodeRest(null));
    when(qrCodeService.generatePng(null))
        .thenThrow(new IllegalArgumentException("Found empty contents"));

    mockMvc
        .perform(get(BASE_URL + "/" + ITEM_ID + "/qr-code/image"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.errors.code").value("QR_CODE_MISSING"))
        .andExpect(jsonPath("$.errors.message").value("QR code indisponible"))
        .andExpect(jsonPath("$.errors.detail", containsString(ITEM_ID)));
  }

  @Test
  void returnsGenerationFailedOnIoError() throws Exception {
    when(itemService.getQrCodeByItemId(ITEM_ID)).thenReturn(qrCodeRest(QR_CONTENT));
    when(qrCodeService.generatePng(QR_CONTENT)).thenThrow(new IOException("flux coupé"));

    mockMvc
        .perform(get(BASE_URL + "/" + ITEM_ID + "/qr-code/image"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.errors.code").value("QR_CODE_GENERATION_FAILED"))
        .andExpect(jsonPath("$.errors.message").value("Génération du QR code impossible"))
        .andExpect(jsonPath("$.errors.detail", containsString(ITEM_ID)));
  }

  @Test
  void returnsGenerationFailedOnEncodingError() throws Exception {
    when(itemService.getQrCodeByItemId(ITEM_ID)).thenReturn(qrCodeRest(QR_CONTENT));
    when(qrCodeService.generatePng(QR_CONTENT))
        .thenThrow(new WriterException("encodage impossible"));

    mockMvc
        .perform(get(BASE_URL + "/" + ITEM_ID + "/qr-code/image"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.errors.code").value("QR_CODE_GENERATION_FAILED"))
        .andExpect(jsonPath("$.errors.message").value("Génération du QR code impossible"))
        .andExpect(jsonPath("$.errors.detail", containsString(ITEM_ID)));
  }
}
