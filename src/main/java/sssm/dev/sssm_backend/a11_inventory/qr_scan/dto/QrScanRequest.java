package sssm.dev.sssm_backend.a11_inventory.qr_scan.dto;

import jakarta.validation.constraints.NotBlank;

public record QrScanRequest(
    @NotBlank(message = "Le code QR est obligatoire") String qrCode) {}
