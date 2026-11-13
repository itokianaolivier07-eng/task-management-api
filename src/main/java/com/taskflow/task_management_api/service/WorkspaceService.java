package com.taskflow.task_management_api.service;

import com.taskflow.task_management_api.dto.*;
import com.taskflow.task_management_api.entity.*;
import com.taskflow.task_management_api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final AccessService access;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;
    private final TaskRepository taskRepository;

    private WorkspaceResponse toResponse(Workspace w, Role role) {
        return new WorkspaceResponse(w.getId(), w.getName(), w.getDescription(), role);
    }

    private MemberResponse toMember(WorkspaceMember m) {
        return new MemberResponse(m.getId(), UserSummary.of(m.getUser()), m.getRole());
    }

    private WorkspaceMember getMember(Workspace workspace, Long memberId) {
        WorkspaceMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membre introuvable"));
        if (!member.getWorkspace().getId().equals(workspace.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Membre introuvable");
        }
        return member;
    }

    @Transactional
    public WorkspaceResponse create(String email, WorkspaceRequest request) {
        User user = access.getUser(email);
        Workspace workspace = workspaceRepository.save(Workspace.builder()
                .name(request.name())
                .description(request.description())
                .owner(user)
                .build());
        memberRepository.save(WorkspaceMember.builder()
                .workspace(workspace).user(user).role(Role.OWNER).build());
        return toResponse(workspace, Role.OWNER);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> listMine(String email) {
        return memberRepository.findByUser(access.getUser(email)).stream()
                .map(m -> toResponse(m.getWorkspace(), m.getRole()))
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkspaceResponse get(String email, Long workspaceId) {
        Workspace workspace = access.getWorkspace(workspaceId);
        WorkspaceMember me = access.requireMember(workspace, access.getUser(email));
        return toResponse(workspace, me.getRole());
    }

    @Transactional
    public WorkspaceResponse update(String email, Long workspaceId, WorkspaceRequest request) {
        Workspace workspace = access.getWorkspace(workspaceId);
        WorkspaceMember me = access.requireAdmin(workspace, access.getUser(email));
        workspace.setName(request.name());
        workspace.setDescription(request.description());
        return toResponse(workspaceRepository.save(workspace), me.getRole());
    }

    @Transactional
    public void delete(String email, Long workspaceId) {
        Workspace workspace = access.getWorkspace(workspaceId);
        WorkspaceMember me = access.requireMember(workspace, access.getUser(email));
        if (me.getRole() != Role.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seul le OWNER peut supprimer le workspace");
        }
        projectService.deleteAllInWorkspace(workspace);
        memberRepository.deleteByWorkspace(workspace);
        workspaceRepository.delete(workspace);
    }

    @Transactional
    public MemberResponse addMember(String email, Long workspaceId, AddMemberRequest request) {
        User requester = access.getUser(email);
        Workspace workspace = access.getWorkspace(workspaceId);

        WorkspaceMember me = memberRepository.findByWorkspaceAndUser(workspace, requester)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé"));
        if (me.getRole() == Role.MEMBER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seuls OWNER et ADMIN peuvent inviter");
        }
        if (request.role() == Role.OWNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le rôle OWNER ne peut pas être attribué");
        }
        if (me.getRole() == Role.ADMIN && request.role() != Role.MEMBER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Un ADMIN ne peut inviter qu'avec le rôle MEMBER");
        }

        User invited = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
        if (memberRepository.findByWorkspaceAndUser(workspace, invited).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Déjà membre de ce workspace");
        }

        WorkspaceMember saved = memberRepository.save(WorkspaceMember.builder()
                .workspace(workspace).user(invited).role(request.role()).build());
        return toMember(saved);
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(String email, Long workspaceId) {
        Workspace workspace = access.getWorkspace(workspaceId);
        access.requireMember(workspace, access.getUser(email));
        return memberRepository.findByWorkspace(workspace).stream().map(this::toMember).toList();
    }

    @Transactional
    public MemberResponse updateMemberRole(String email, Long workspaceId, Long memberId,
                                           UpdateMemberRoleRequest request) {
        Workspace workspace = access.getWorkspace(workspaceId);
        WorkspaceMember me = access.requireMember(workspace, access.getUser(email));
        if (me.getRole() != Role.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seul le OWNER peut changer les rôles");
        }
        WorkspaceMember target = getMember(workspace, memberId);
        if (target.getRole() == Role.OWNER || request.role() == Role.OWNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le rôle OWNER ne peut être ni modifié ni attribué");
        }
        target.setRole(request.role());
        return toMember(memberRepository.save(target));
    }

    @Transactional
    public void removeMember(String email, Long workspaceId, Long memberId) {
        Workspace workspace = access.getWorkspace(workspaceId);
        WorkspaceMember me = access.requireAdmin(workspace, access.getUser(email));
        WorkspaceMember target = getMember(workspace, memberId);
        if (target.getRole() == Role.OWNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le OWNER ne peut pas être retiré");
        }
        if (me.getRole() == Role.ADMIN && target.getRole() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Un ADMIN ne peut pas retirer un autre ADMIN");
        }
        Long targetId = target.getId();
        taskRepository.unassignUserInWorkspace(workspace.getId(), target.getUser().getId());
        memberRepository.deleteById(targetId);
    }
}
