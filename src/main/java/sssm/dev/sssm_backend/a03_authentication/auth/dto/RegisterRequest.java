package sssm.dev.sssm_backend.a03_authentication.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Size(max = 100) String username,
    @Email @Size(max = 180) String email,
    @NotBlank @Size(min = 8, max = 128) String password,
    @Size(max = 100) String firstName,
    @Size(max = 100) String lastName,
    @Size(max = 50) String phone) {}
