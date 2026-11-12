package com.taskflow.task_management_api.dto;

import com.taskflow.task_management_api.entity.Role;

public record WorkspaceResponse(Long id, String name, String description, Role role) {}