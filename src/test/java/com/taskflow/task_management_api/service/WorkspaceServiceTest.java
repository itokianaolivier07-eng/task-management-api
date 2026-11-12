package com.taskflow.task_management_api.service;

import com.taskflow.task_management_api.dto.AddMemberRequest;
import com.taskflow.task_management_api.dto.WorkspaceRequest;
import com.taskflow.task_management_api.dto.WorkspaceResponse;
import com.taskflow.task_management_api.entity.*;
import com.taskflow.task_management_api.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkspaceServiceTest {

    @Mock UserRepository userRepository;
    @Mock WorkspaceRepository workspaceRepository;
    @Mock WorkspaceMemberRepository memberRepository;
    @Mock ProjectService projectService;
    @Mock TaskRepository taskRepository;

    WorkspaceService service;
    User owner, member, invited;
    Workspace workspace;

    @BeforeEach
    void setUp() {
        AccessService access = new AccessService(userRepository, workspaceRepository, memberRepository);
        service = new WorkspaceService(access, workspaceRepository, memberRepository,
                userRepository, projectService, taskRepository);

        owner = User.builder().id(1L).name("Owner").email("owner@x.com").password("x").build();
        member = User.builder().id(2L).name("Member").email("member@x.com").password("x").build();
        invited = User.builder().id(3L).name("Invited").email("invited@x.com").password("x").build();
        workspace = Workspace.builder().id(1L).name("WS").owner(owner).build();

        when(userRepository.findByEmail("owner@x.com")).thenReturn(Optional.of(owner));
        when(userRepository.findByEmail("member@x.com")).thenReturn(Optional.of(member));
        when(userRepository.findByEmail("invited@x.com")).thenReturn(Optional.of(invited));
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
        when(memberRepository.findByWorkspaceAndUser(workspace, owner))
                .thenReturn(Optional.of(WorkspaceMember.builder().id(10L).workspace(workspace).user(owner).role(Role.OWNER).build()));
        when(memberRepository.findByWorkspaceAndUser(workspace, member))
                .thenReturn(Optional.of(WorkspaceMember.builder().id(11L).workspace(workspace).user(member).role(Role.MEMBER).build()));
        when(memberRepository.save(any(WorkspaceMember.class))).thenAnswer(i -> i.getArgument(0));
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void creatorBecomesOwner() {
        WorkspaceResponse response = service.create("owner@x.com", new WorkspaceRequest("New", "desc"));

        assertEquals(Role.OWNER, response.role());
        verify(memberRepository).save(argThat((WorkspaceMember m) -> m.getRole() == Role.OWNER));
    }

    @Test
    void plainMemberCannotInvite() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.addMember("member@x.com", 1L, new AddMemberRequest("invited@x.com", Role.MEMBER)));

        assertEquals(403, ex.getStatusCode().value());
        verify(memberRepository, never()).save(any());
    }

    @Test
    void ownerCanInvite() {
        var response = service.addMember("owner@x.com", 1L, new AddMemberRequest("invited@x.com", Role.ADMIN));

        assertEquals(Role.ADMIN, response.role());
        assertEquals("invited@x.com", response.user().email());
    }

    @Test
    void cannotInviteSomeoneAsOwner() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.addMember("owner@x.com", 1L, new AddMemberRequest("invited@x.com", Role.OWNER)));

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void cannotInviteExistingMember() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.addMember("owner@x.com", 1L, new AddMemberRequest("member@x.com", Role.MEMBER)));

        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void onlyOwnerCanDeleteWorkspace() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.delete("member@x.com", 1L));

        assertEquals(403, ex.getStatusCode().value());
        verify(workspaceRepository, never()).delete(any());
    }
}
