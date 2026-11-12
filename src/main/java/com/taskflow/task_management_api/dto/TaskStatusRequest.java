package com.taskflow.task_management_api.dto;

import com.taskflow.task_management_api.entity.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record TaskStatusRequest(@NotNull TaskStatus status) {}
