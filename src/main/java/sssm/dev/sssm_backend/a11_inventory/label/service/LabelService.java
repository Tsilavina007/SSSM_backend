package sssm.dev.sssm_backend.a11_inventory.label.service;

import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import sssm.dev.sssm_backend.a00_infrastructure.exception.NotFoundException;
import sssm.dev.sssm_backend.a03_authentication.auth.security.CurrentUser;
import sssm.dev.sssm_backend.a11_inventory.item.service.QrCodeService;
import sssm.dev.sssm_backend.a11_inventory.label.dto.QrLabelData;
import sssm.dev.sssm_backend.a11_inventory.label.repository.LabelDAO;

/**
 * Production des étiquettes.
 *
 * <p>Les étiquettes sont rendues en HTML imprimable : le QR code est intégré directement (donnée
 * base64) et le texte est lisible, ce qui évite toute dépendance supplémentaire pour produire une
 * planche. Un lot de cent étiquettes est rendu en un seul appel.
 */
@Service
@RequiredArgsConstructor
public class LabelService {

  private static final int DEFAULT_SIZE = 300;

  private final LabelDAO labelDAO;
  private final QrCodeService qrCodeService;
  private final CurrentUser currentUser;

  /** Étiquette d'un article. */
  public String renderLabel(String itemId, Integer size) {
    String companyId = currentUser.requireCompanyId();
    QrLabelData data =
        labelDAO
            .findLabel(itemId, companyId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Article introuvable",
                        "Aucun article ne correspond à l'identifiant " + itemId + ".",
                        "NOT_FOUND"));
    return renderPage(List.of(data), size == null ? DEFAULT_SIZE : size);
  }

  /** Planche d'étiquettes. */
  public String renderBatch(List<String> itemIds, Integer size) {
    String companyId = currentUser.requireCompanyId();
    List<QrLabelData> labels = labelDAO.findLabels(itemIds, companyId);
    return renderPage(labels, size == null ? DEFAULT_SIZE : size);
  }

  private String renderPage(List<QrLabelData> labels, int size) {
    StringBuilder html = new StringBuilder();
    html.append("<!DOCTYPE html><html lang=\"fr\"><head><meta charset=\"utf-8\">");
    html.append("<title>Étiquettes</title><style>");
    html.append(
        "body{font-family:Arial,Helvetica,sans-serif;margin:12px;color:#111}"
            + ".sheet{display:flex;flex-wrap:wrap;gap:8px}"
            + ".label{width:320px;border:1px solid #111;border-radius:6px;padding:8px;"
            + "display:flex;align-items:center;gap:8px;page-break-inside:avoid}"
            + ".label img{width:96px;height:96px}"
            + ".lines{font-size:12px;line-height:1.35}"
            + ".code{font-family:monospace;font-size:15px;font-weight:bold;letter-spacing:1px}"
            + ".name{font-weight:bold}.muted{color:#444}"
            + "@page{size:A4;margin:8mm}@media print{.label{border:1px dashed #999}}");
    html.append("</style></head><body><div class=\"sheet\">");
    for (QrLabelData label : labels) {
      html.append(renderLabelCell(label, size));
    }
    html.append("</div></body></html>");
    return html.toString();
  }

  private String renderLabelCell(QrLabelData label, int size) {
    StringBuilder cell = new StringBuilder();
    cell.append("<div class=\"label\">");
    cell.append("<img alt=\"QR code ").append(escape(label.getQr_code())).append("\" src=\"");
    cell.append(dataUri(label.getQr_code(), size));
    cell.append("\">");
    cell.append("<div class=\"lines\">");
    cell.append("<div class=\"code\">").append(escape(label.getQr_code())).append("</div>");
    if (label.getType_name() != null) {
      cell
          .append("<div class=\"name\">")
          .append(escape(label.getType_code()))
          .append(" — ")
          .append(escape(label.getType_name()))
          .append("</div>");
    }
    if (label.getDescription() != null && !label.getDescription().isBlank()) {
      cell.append("<div>").append(escape(label.getDescription())).append("</div>");
    }
    if (label.getCategory_name() != null) {
      cell.append("<div class=\"muted\">").append(escape(label.getCategory_name())).append("</div>");
    }
    if (label.getWarehouse_name() != null) {
      cell.append("<div class=\"muted\">").append(escape(label.getWarehouse_name())).append("</div>");
    }
    if (label.getCompany_name() != null) {
      cell.append("<div class=\"muted\">").append(escape(label.getCompany_name())).append("</div>");
    }
    cell.append("</div></div>");
    return cell.toString();
  }

  private String dataUri(String qrCode, int size) {
    try {
      byte[] png = qrCodeService.generatePng(qrCode, size, size);
      return "data:image/png;base64," + Base64.getEncoder().encodeToString(png);
    } catch (Exception exception) {
      // Une étiquette sans image reste imprimable : le code texte est présent.
      return "";
    }
  }

  /** Échappe le texte affiché dans la planche. */
  private static String escape(String value) {
    if (value == null) {
      return "";
    }
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }
}
