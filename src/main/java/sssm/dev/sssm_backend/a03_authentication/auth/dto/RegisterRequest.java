package sssm.dev.sssm_backend.a03_authentication.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Le nom d'utilisateur est obligatoire")
    @Size(max = 100, message = "100 caractères maximum")
    String username,

    @Email(message = "L'adresse email est invalide")
    @Size(max = 180, message = "180 caractères maximum")
    String email,

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, max = 128,
        message = "Le mot de passe doit contenir entre 8 et 128 caractères")
    String password,

    @Size(max = 100) String firstName,
    @Size(max = 100, message = "100 caractères maximum")
    String lastName,
    @Size(max = 50, message = "50 caractères maximum")
    String phone
) {}
