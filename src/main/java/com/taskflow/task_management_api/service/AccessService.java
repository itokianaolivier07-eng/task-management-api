package com.taskflow.task_management_api.service;

import com.taskflow.task_management_api.entity.*;
import com.taskflow.task_management_api.repository.UserRepository;
import com.taskflow.task_management_api.repository.WorkspaceMemberRepository;
import com.taskflow.task_management_api.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Point central des vérifications d'identité et de permissions. */
@Service
@RequiredArgsConstructor
public class AccessService {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;

    public User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
    }

    public Workspace getWorkspace(Long id) {
        return workspaceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace introuvable"));
    }

    public WorkspaceMember requireMember(Workspace workspace, User user) {
        return memberRepository.findByWorkspaceAndUser(workspace, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé"));
    }

    public WorkspaceMember requireAdmin(Workspace workspace, User user) {
        WorkspaceMember member = requireMember(workspace, user);
        if (!isAdmin(member)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Réservé aux rôles OWNER et ADMIN");
        }
        return member;
    }

    public boolean isMember(Workspace workspace, User user) {
        return memberRepository.findByWorkspaceAndUser(workspace, user).isPresent();
    }

    public static boolean isAdmin(WorkspaceMember member) {
        return member.getRole() == Role.OWNER || member.getRole() == Role.ADMIN;
    }
}
