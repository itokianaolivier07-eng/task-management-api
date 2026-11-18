package com.taskflow.task_management_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Locale;

public record EmailRequest(@NotBlank @Email String email) {
    public EmailRequest {
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
