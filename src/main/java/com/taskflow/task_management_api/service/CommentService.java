package com.taskflow.task_management_api.service;

import com.taskflow.task_management_api.dto.CommentRequest;
import com.taskflow.task_management_api.dto.CommentResponse;
import com.taskflow.task_management_api.dto.UserSummary;
import com.taskflow.task_management_api.entity.Comment;
import com.taskflow.task_management_api.entity.Task;
import com.taskflow.task_management_api.entity.User;
import com.taskflow.task_management_api.entity.WorkspaceMember;
import com.taskflow.task_management_api.repository.CommentRepository;
import com.taskflow.task_management_api.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final AccessService access;
    private final NotificationService notificationService;

    private Task getTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tâche introuvable"));
    }

    private CommentResponse toResponse(Comment c) {
        return new CommentResponse(c.getId(), c.getTask().getId(), UserSummary.of(c.getAuthor()),
                c.getContent(), c.getCreatedAt());
    }

    @Transactional
    public CommentResponse add(String email, Long taskId, CommentRequest request) {
        User author = access.getUser(email);
        Task task = getTask(taskId);
        access.requireMember(task.getProject().getWorkspace(), author);

        Comment comment = commentRepository.save(Comment.builder()
                .task(task).author(author).content(request.content()).build());

        // Prévenir la personne assignée et le créateur de la tâche (sauf l'auteur du commentaire).
        Map<Long, User> recipients = new LinkedHashMap<>();
        if (task.getAssignee() != null) {
            recipients.put(task.getAssignee().getId(), task.getAssignee());
        }
        recipients.put(task.getCreatedBy().getId(), task.getCreatedBy());
        recipients.remove(author.getId());
        recipients.values().forEach(u -> notificationService.send(
                u.getEmail(),
                "Nouveau commentaire sur : " + task.getTitle(),
                "Bonjour " + u.getName() + ",\n\n"
                        + author.getName() + " a commenté la tâche « " + task.getTitle() + " » :\n\n"
                        + request.content() + "\n"));

        return toResponse(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> list(String email, Long taskId) {
        User user = access.getUser(email);
        Task task = getTask(taskId);
        access.requireMember(task.getProject().getWorkspace(), user);
        return commentRepository.findAllByTask(taskId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public void delete(String email, Long commentId) {
        User user = access.getUser(email);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Commentaire introuvable"));
        WorkspaceMember me = access.requireMember(comment.getTask().getProject().getWorkspace(), user);
        boolean isAuthor = Objects.equals(comment.getAuthor().getId(), user.getId());
        if (!(isAuthor || AccessService.isAdmin(me))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Seul l'auteur ou un admin peut supprimer ce commentaire");
        }
        commentRepository.delete(comment);
    }
}
