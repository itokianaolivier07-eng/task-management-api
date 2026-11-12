package com.taskflow.task_management_api.dto;

import com.taskflow.task_management_api.entity.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(@NotNull Role role) {}
