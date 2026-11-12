package com.taskflow.task_management_api.service;

import com.taskflow.task_management_api.dto.ProjectRequest;
import com.taskflow.task_management_api.dto.ProjectResponse;
import com.taskflow.task_management_api.entity.Project;
import com.taskflow.task_management_api.entity.User;
import com.taskflow.task_management_api.entity.Workspace;
import com.taskflow.task_management_api.repository.CommentRepository;
import com.taskflow.task_management_api.repository.ProjectRepository;
import com.taskflow.task_management_api.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final CommentRepository commentRepository;
    private final AccessService access;

    private ProjectResponse toResponse(Project p) {
        return new ProjectResponse(p.getId(), p.getName(), p.getDescription(),
                p.getWorkspace().getId(), p.getCreatedAt());
    }

    private Project getProject(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Projet introuvable"));
    }

    @Transactional
    public ProjectResponse create(String email, Long workspaceId, ProjectRequest request) {
        User user = access.getUser(email);
        Workspace workspace = access.getWorkspace(workspaceId);
        access.requireAdmin(workspace, user);
        Project project = projectRepository.save(Project.builder()
                .name(request.name())
                .description(request.description())
                .workspace(workspace)
                .createdBy(user)
                .build());
        return toResponse(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> list(String email, Long workspaceId) {
        User user = access.getUser(email);
        Workspace workspace = access.getWorkspace(workspaceId);
        access.requireMember(workspace, user);
        return projectRepository.findByWorkspaceOrderByCreatedAtDesc(workspace).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(String email, Long projectId) {
        User user = access.getUser(email);
        Project project = getProject(projectId);
        access.requireMember(project.getWorkspace(), user);
        return toResponse(project);
    }

    @Transactional
    public ProjectResponse update(String email, Long projectId, ProjectRequest request) {
        User user = access.getUser(email);
        Project project = getProject(projectId);
        access.requireAdmin(project.getWorkspace(), user);
        project.setName(request.name());
        project.setDescription(request.description());
        return toResponse(projectRepository.save(project));
    }

    @Transactional
    public void delete(String email, Long projectId) {
        User user = access.getUser(email);
        Project project = getProject(projectId);
        access.requireAdmin(project.getWorkspace(), user);
        deleteWithContent(project.getId());
    }

    /** Utilisé lors de la suppression d'un workspace. */
    @Transactional
    public void deleteAllInWorkspace(Workspace workspace) {
        List<Long> ids = projectRepository.findByWorkspaceOrderByCreatedAtDesc(workspace).stream()
                .map(Project::getId).toList();
        ids.forEach(this::deleteWithContent);
    }

    private void deleteWithContent(Long projectId) {
        commentRepository.deleteByProjectId(projectId);
        taskRepository.deleteByProjectId(projectId);
        projectRepository.deleteById(projectId);
    }
}
