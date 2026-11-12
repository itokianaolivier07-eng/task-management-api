package com.taskflow.task_management_api.service;

import com.taskflow.task_management_api.dto.TaskRequest;
import com.taskflow.task_management_api.dto.TaskResponse;
import com.taskflow.task_management_api.dto.UserSummary;
import com.taskflow.task_management_api.entity.*;
import com.taskflow.task_management_api.repository.CommentRepository;
import com.taskflow.task_management_api.repository.ProjectRepository;
import com.taskflow.task_management_api.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final CommentRepository commentRepository;
    private final AccessService access;
    private final NotificationService notificationService;

    // ---------- helpers ----------

    private Project getProject(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Projet introuvable"));
    }

    private Task getTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tâche introuvable"));
    }

    private TaskResponse toResponse(Task t) {
        return new TaskResponse(t.getId(), t.getTitle(), t.getDescription(), t.getStatus(), t.getPriority(),
                t.getDueDate(), t.getProject().getId(), UserSummary.of(t.getAssignee()),
                UserSummary.of(t.getCreatedBy()), t.getCreatedAt());
    }

    private static Long idOf(User u) {
        return u == null ? null : u.getId();
    }

    private User resolveAssignee(Workspace workspace, String assigneeEmail) {
        if (assigneeEmail == null || assigneeEmail.isBlank()) {
            return null;
        }
        User assignee = access.getUser(assigneeEmail);
        if (!access.isMember(workspace, assignee)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "L'utilisateur assigné n'est pas membre du workspace");
        }
        return assignee;
    }

    /** Admin du workspace, créateur ou personne assignée. */
    private void requireCanModify(Task task, User actor, WorkspaceMember me) {
        boolean isCreator = Objects.equals(idOf(task.getCreatedBy()), actor.getId());
        boolean isAssignee = Objects.equals(idOf(task.getAssignee()), actor.getId());
        if (!(AccessService.isAdmin(me) || isCreator || isAssignee)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Seul un admin, le créateur ou la personne assignée peut modifier cette tâche");
        }
    }

    private void notifyAssigned(Task task, User actor) {
        User assignee = task.getAssignee();
        if (assignee == null || Objects.equals(assignee.getId(), actor.getId())) {
            return;
        }
        String due = task.getDueDate() != null ? task.getDueDate().toString() : "aucune";
        notificationService.send(
                assignee.getEmail(),
                "Nouvelle tâche assignée : " + task.getTitle(),
                "Bonjour " + assignee.getName() + ",\n\n"
                        + actor.getName() + " vous a assigné la tâche « " + task.getTitle() + " »\n"
                        + "Projet : " + task.getProject().getName() + "\n"
                        + "Priorité : " + task.getPriority() + "\n"
                        + "Échéance : " + due + "\n");
    }

    // ---------- use cases ----------

    @Transactional
    public TaskResponse create(String email, Long projectId, TaskRequest request) {
        User actor = access.getUser(email);
        Project project = getProject(projectId);
        Workspace workspace = project.getWorkspace();
        access.requireMember(workspace, actor);
        User assignee = resolveAssignee(workspace, request.assigneeEmail());

        Task task = taskRepository.save(Task.builder()
                .title(request.title())
                .description(request.description())
                .priority(request.priority() != null ? request.priority() : Priority.MEDIUM)
                .status(TaskStatus.TODO)
                .dueDate(request.dueDate())
                .project(project)
                .assignee(assignee)
                .createdBy(actor)
                .build());

        notifyAssigned(task, actor);
        return toResponse(task);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> list(String email, Long projectId, TaskStatus status,
                                   Priority priority, Long assigneeId) {
        User actor = access.getUser(email);
        Project project = getProject(projectId);
        access.requireMember(project.getWorkspace(), actor);
        return taskRepository.findAllByProject(projectId).stream()
                .filter(t -> status == null || t.getStatus() == status)
                .filter(t -> priority == null || t.getPriority() == priority)
                .filter(t -> assigneeId == null || Objects.equals(idOf(t.getAssignee()), assigneeId))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> mine(String email) {
        User actor = access.getUser(email);
        return taskRepository.findAssignedTo(actor.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse get(String email, Long taskId) {
        User actor = access.getUser(email);
        Task task = getTask(taskId);
        access.requireMember(task.getProject().getWorkspace(), actor);
        return toResponse(task);
    }

    @Transactional
    public TaskResponse update(String email, Long taskId, TaskRequest request) {
        User actor = access.getUser(email);
        Task task = getTask(taskId);
        Workspace workspace = task.getProject().getWorkspace();
        WorkspaceMember me = access.requireMember(workspace, actor);
        requireCanModify(task, actor, me);

        User newAssignee = resolveAssignee(workspace, request.assigneeEmail());
        boolean assigneeChanged = !Objects.equals(idOf(task.getAssignee()), idOf(newAssignee));
        if (!Objects.equals(task.getDueDate(), request.dueDate())) {
            task.setReminderSent(false);
        }

        task.setTitle(request.title());
        task.setDescription(request.description());
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        task.setDueDate(request.dueDate());
        task.setAssignee(newAssignee);
        taskRepository.save(task);

        if (assigneeChanged) {
            notifyAssigned(task, actor);
        }
        return toResponse(task);
    }

    @Transactional
    public TaskResponse updateStatus(String email, Long taskId, TaskStatus status) {
        User actor = access.getUser(email);
        Task task = getTask(taskId);
        WorkspaceMember me = access.requireMember(task.getProject().getWorkspace(), actor);
        requireCanModify(task, actor, me);
        task.setStatus(status);
        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public void delete(String email, Long taskId) {
        User actor = access.getUser(email);
        Task task = getTask(taskId);
        WorkspaceMember me = access.requireMember(task.getProject().getWorkspace(), actor);
        boolean isCreator = Objects.equals(idOf(task.getCreatedBy()), actor.getId());
        if (!(AccessService.isAdmin(me) || isCreator)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Seul un admin ou le créateur peut supprimer cette tâche");
        }
        commentRepository.deleteByTaskId(taskId);
        taskRepository.deleteById(taskId);
    }
}
