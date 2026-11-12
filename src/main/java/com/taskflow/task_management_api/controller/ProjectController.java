package com.taskflow.task_management_api.controller;

import com.taskflow.task_management_api.dto.ProjectRequest;
import com.taskflow.task_management_api.dto.ProjectResponse;
import com.taskflow.task_management_api.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping("/workspaces/{workspaceId}/projects")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(Authentication auth, @PathVariable Long workspaceId,
                                  @Valid @RequestBody ProjectRequest request) {
        return projectService.create(auth.getName(), workspaceId, request);
    }

    @GetMapping("/workspaces/{workspaceId}/projects")
    public List<ProjectResponse> list(Authentication auth, @PathVariable Long workspaceId) {
        return projectService.list(auth.getName(), workspaceId);
    }

    @GetMapping("/projects/{id}")
    public ProjectResponse get(Authentication auth, @PathVariable Long id) {
        return projectService.get(auth.getName(), id);
    }

    @PutMapping("/projects/{id}")
    public ProjectResponse update(Authentication auth, @PathVariable Long id,
                                  @Valid @RequestBody ProjectRequest request) {
        return projectService.update(auth.getName(), id, request);
    }

    @DeleteMapping("/projects/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication auth, @PathVariable Long id) {
        projectService.delete(auth.getName(), id);
    }
}
