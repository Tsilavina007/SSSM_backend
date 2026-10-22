package sssm.dev.sssm_backend.a11_inventory.qr_scan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Requête de résolution d'un QR code.
 *
 * <p>{@code deviceId} est facultatif : il identifie le terminal qui scanne et alimente le journal
 * des scans.
 */
public record QrScanRequest(
    @NotBlank(message = "Le code QR est obligatoire") @Size(max = 100, message = "100 caractères maximum") String qrCode,
    @Size(max = 100, message = "100 caractères maximum") String deviceId) {}
