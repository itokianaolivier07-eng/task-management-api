package com.taskflow.task_management_api.controller;

import com.taskflow.task_management_api.dto.*;
import com.taskflow.task_management_api.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkspaceResponse create(Authentication auth, @Valid @RequestBody WorkspaceRequest request) {
        return workspaceService.create(auth.getName(), request);
    }

    @GetMapping
    public List<WorkspaceResponse> listMine(Authentication auth) {
        return workspaceService.listMine(auth.getName());
    }

    @GetMapping("/{id}")
    public WorkspaceResponse get(Authentication auth, @PathVariable Long id) {
        return workspaceService.get(auth.getName(), id);
    }

    @PutMapping("/{id}")
    public WorkspaceResponse update(Authentication auth, @PathVariable Long id,
                                    @Valid @RequestBody WorkspaceRequest request) {
        return workspaceService.update(auth.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication auth, @PathVariable Long id) {
        workspaceService.delete(auth.getName(), id);
    }

    @PostMapping("/{id}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse addMember(Authentication auth, @PathVariable Long id,
                                    @Valid @RequestBody AddMemberRequest request) {
        return workspaceService.addMember(auth.getName(), id, request);
    }

    @GetMapping("/{id}/members")
    public List<MemberResponse> listMembers(Authentication auth, @PathVariable Long id) {
        return workspaceService.listMembers(auth.getName(), id);
    }

    @PatchMapping("/{id}/members/{memberId}")
    public MemberResponse updateMemberRole(Authentication auth, @PathVariable Long id,
                                           @PathVariable Long memberId,
                                           @Valid @RequestBody UpdateMemberRoleRequest request) {
        return workspaceService.updateMemberRole(auth.getName(), id, memberId, request);
    }

    @DeleteMapping("/{id}/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(Authentication auth, @PathVariable Long id, @PathVariable Long memberId) {
        workspaceService.removeMember(auth.getName(), id, memberId);
    }
}
