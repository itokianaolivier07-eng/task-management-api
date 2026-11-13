package com.taskflow.task_management_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WorkspaceRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 255) String description) {}
