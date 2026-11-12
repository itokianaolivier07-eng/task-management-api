package com.taskflow.task_management_api.dto;

import java.time.LocalDateTime;

public record ProjectResponse(Long id, String name, String description, Long workspaceId, LocalDateTime createdAt) {}
