package com.taskflow.task_management_api.dto;

import jakarta.validation.constraints.*;

import java.util.Locale;

public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password) {

    public LoginRequest {
        // Doit correspondre à l'e-mail normalisé en base lors de l'inscription.
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
