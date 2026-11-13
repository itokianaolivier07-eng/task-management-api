package com.taskflow.task_management_api.dto;

import jakarta.validation.constraints.*;

import java.util.Locale;

public record RegisterRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password) {

    public RegisterRequest {
        // Les e-mails sont stockés et comparés en minuscules : évite les doublons liés à la casse.
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
