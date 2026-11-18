package com.taskflow.task_management_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TokenRequest(@NotBlank @Size(max = 200) String token) {}
