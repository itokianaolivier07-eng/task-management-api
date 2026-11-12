package com.taskflow.task_management_api.dto;

import java.time.LocalDateTime;

public record ApiError(LocalDateTime timestamp, int status, String message) {}
