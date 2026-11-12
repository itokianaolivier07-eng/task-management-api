package com.taskflow.task_management_api.dto;

import com.taskflow.task_management_api.entity.Role;

public record MemberResponse(Long id, UserSummary user, Role role) {}
