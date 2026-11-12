package com.taskflow.task_management_api.controller;

import com.taskflow.task_management_api.dto.CommentRequest;
import com.taskflow.task_management_api.dto.CommentResponse;
import com.taskflow.task_management_api.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/tasks/{taskId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse add(Authentication auth, @PathVariable Long taskId,
                               @Valid @RequestBody CommentRequest request) {
        return commentService.add(auth.getName(), taskId, request);
    }

    @GetMapping("/tasks/{taskId}/comments")
    public List<CommentResponse> list(Authentication auth, @PathVariable Long taskId) {
        return commentService.list(auth.getName(), taskId);
    }

    @DeleteMapping("/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication auth, @PathVariable Long id) {
        commentService.delete(auth.getName(), id);
    }
}
