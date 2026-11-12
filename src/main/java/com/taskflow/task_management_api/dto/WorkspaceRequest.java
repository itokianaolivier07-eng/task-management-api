package com.taskflow.task_management_api.dto;

import jakarta.validation.constraints.NotBlank;

public record WorkspaceRequest(@NotBlank String name, String description) {}