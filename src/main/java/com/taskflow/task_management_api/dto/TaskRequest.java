package com.taskflow.task_management_api.dto;

import com.taskflow.task_management_api.entity.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Locale;

/** priority : MEDIUM par défaut. assigneeEmail : optionnel (doit être membre du workspace). */
public record TaskRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 4000) String description,
        Priority priority,
        LocalDate dueDate,
        String assigneeEmail) {

    public TaskRequest {
        assigneeEmail = assigneeEmail == null ? null : assigneeEmail.trim().toLowerCase(Locale.ROOT);
    }
}
