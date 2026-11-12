package com.taskflow.task_management_api.dto;

import java.time.LocalDateTime;

public record CommentResponse(Long id, Long taskId, UserSummary author, String content, LocalDateTime createdAt) {}
