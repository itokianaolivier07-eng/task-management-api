package com.taskflow.task_management_api.dto;

import com.taskflow.task_management_api.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddMemberRequest(@NotBlank @Email String email, @NotNull Role role) {}