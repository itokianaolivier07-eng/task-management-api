package com.taskflow.task_management_api.controller;

import com.taskflow.task_management_api.dto.TaskRequest;
import com.taskflow.task_management_api.dto.TaskResponse;
import com.taskflow.task_management_api.dto.TaskStatusRequest;
import com.taskflow.task_management_api.entity.Priority;
import com.taskflow.task_management_api.entity.TaskStatus;
import com.taskflow.task_management_api.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping("/projects/{projectId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(Authentication auth, @PathVariable Long projectId,
                               @Valid @RequestBody TaskRequest request) {
        return taskService.create(auth.getName(), projectId, request);
    }

    @GetMapping("/projects/{projectId}/tasks")
    public List<TaskResponse> list(Authentication auth, @PathVariable Long projectId,
                                   @RequestParam(required = false) TaskStatus status,
                                   @RequestParam(required = false) Priority priority,
                                   @RequestParam(required = false) Long assigneeId) {
        return taskService.list(auth.getName(), projectId, status, priority, assigneeId);
    }

    @GetMapping("/tasks/mine")
    public List<TaskResponse> mine(Authentication auth) {
        return taskService.mine(auth.getName());
    }

    @GetMapping("/tasks/{id}")
    public TaskResponse get(Authentication auth, @PathVariable Long id) {
        return taskService.get(auth.getName(), id);
    }

    @PutMapping("/tasks/{id}")
    public TaskResponse update(Authentication auth, @PathVariable Long id,
                               @Valid @RequestBody TaskRequest request) {
        return taskService.update(auth.getName(), id, request);
    }

    @PatchMapping("/tasks/{id}/status")
    public TaskResponse updateStatus(Authentication auth, @PathVariable Long id,
                                     @Valid @RequestBody TaskStatusRequest request) {
        return taskService.updateStatus(auth.getName(), id, request.status());
    }

    @DeleteMapping("/tasks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication auth, @PathVariable Long id) {
        taskService.delete(auth.getName(), id);
    }
}
