package com.taskflow.task_management_api.dto;

import com.taskflow.task_management_api.entity.User;

public record UserSummary(Long id, String name, String email) {
    public static UserSummary of(User u) {
        return u == null ? null : new UserSummary(u.getId(), u.getName(), u.getEmail());
    }
}
