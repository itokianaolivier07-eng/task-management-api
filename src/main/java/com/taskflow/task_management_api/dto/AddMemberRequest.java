package com.taskflow.task_management_api.dto;

import com.taskflow.task_management_api.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Locale;

public record AddMemberRequest(@NotBlank @Email String email, @NotNull Role role) {

    public AddMemberRequest {
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
