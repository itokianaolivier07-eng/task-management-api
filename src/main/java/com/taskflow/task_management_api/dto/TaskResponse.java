package com.taskflow.task_management_api.dto;

import com.taskflow.task_management_api.entity.Priority;
import com.taskflow.task_management_api.entity.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        Priority priority,
        LocalDate dueDate,
        Long projectId,
        UserSummary assignee,
        UserSummary createdBy,
        LocalDateTime createdAt) {}
