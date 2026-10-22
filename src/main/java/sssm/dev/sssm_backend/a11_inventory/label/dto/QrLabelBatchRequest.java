package sssm.dev.sssm_backend.a11_inventory.label.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Demande de planche d'étiquettes.
 *
 * <p>La planche est produite en un seul appel : le navigateur n'a pas à télécharger les images une
 * par une.
 */
public record QrLabelBatchRequest(
    @NotEmpty(message = "La liste des articles est obligatoire")
        @Size(max = 100, message = "100 articles maximum par planche")
        List<String> item_ids,
    @Min(value = 100, message = "La taille doit être comprise entre 100 et 1000 pixels")
        @Max(value = 1000, message = "La taille doit être comprise entre 100 et 1000 pixels")
        Integer size) {}
