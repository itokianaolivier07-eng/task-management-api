package com.taskflow.task_management_api.dto;

import jakarta.validation.constraints.*;

import java.util.Locale;

public record RegisterRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Email @Size(max = 255) String email,
        // max = 72 : au-delà, BCrypt ignore silencieusement le reste du mot de passe.
        @NotBlank @Size(min = 8, max = 72) String password) {

    public RegisterRequest {
        // Les e-mails sont stockés et comparés en minuscules : évite les doublons liés à la casse.
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
