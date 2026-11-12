package com.taskflow.task_management_api.service;

import com.taskflow.task_management_api.dto.TaskRequest;
import com.taskflow.task_management_api.dto.TaskResponse;
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

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskServiceTest {

    @Mock TaskRepository taskRepository;
    @Mock ProjectRepository projectRepository;
    @Mock CommentRepository commentRepository;
    @Mock UserRepository userRepository;
    @Mock WorkspaceRepository workspaceRepository;
    @Mock WorkspaceMemberRepository memberRepository;
    @Mock NotificationService notificationService;

    TaskService service;
    User owner, member, other, outsider;
    Workspace workspace;
    Project project;

    private WorkspaceMember membership(User user, Role role) {
        return WorkspaceMember.builder().workspace(workspace).user(user).role(role).build();
    }

    @BeforeEach
    void setUp() {
        AccessService access = new AccessService(userRepository, workspaceRepository, memberRepository);
        service = new TaskService(taskRepository, projectRepository, commentRepository, access, notificationService);

        owner = User.builder().id(1L).name("Owner").email("owner@x.com").password("x").build();
        member = User.builder().id(2L).name("Member").email("member@x.com").password("x").build();
        other = User.builder().id(3L).name("Other").email("other@x.com").password("x").build();
        outsider = User.builder().id(4L).name("Outsider").email("outsider@x.com").password("x").build();
        workspace = Workspace.builder().id(1L).name("WS").owner(owner).build();
        project = Project.builder().id(10L).name("Projet").workspace(workspace).createdBy(owner).build();

        for (User u : new User[]{owner, member, other, outsider}) {
            when(userRepository.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
        }
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(memberRepository.findByWorkspaceAndUser(workspace, owner)).thenReturn(Optional.of(membership(owner, Role.OWNER)));
        when(memberRepository.findByWorkspaceAndUser(workspace, member)).thenReturn(Optional.of(membership(member, Role.MEMBER)));
        when(memberRepository.findByWorkspaceAndUser(workspace, other)).thenReturn(Optional.of(membership(other, Role.MEMBER)));
        // outsider : Optional.empty() par défaut

        when(taskRepository.save(any(Task.class))).thenAnswer(i -> {
            Task t = i.getArgument(0);
            if (t.getId() == null) t.setId(100L);
            return t;
        });
    }

    private Task existingTask(User assignee) {
        Task task = Task.builder().id(100L).title("T").project(project)
                .createdBy(owner).assignee(assignee).build();
        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
        return task;
    }

    @Test
    void memberCanCreateTaskAndAssigneeIsNotified() {
        TaskRequest request = new TaskRequest("Écrire les tests", "desc", Priority.HIGH,
                LocalDate.now().plusDays(3), "member@x.com");

        TaskResponse response = service.create("owner@x.com", 10L, request);

        assertEquals(TaskStatus.TODO, response.status());
        assertEquals(Priority.HIGH, response.priority());
        assertEquals("member@x.com", response.assignee().email());
        verify(notificationService).send(eq("member@x.com"), anyString(), anyString());
    }

    @Test
    void defaultPriorityIsMedium() {
        TaskResponse response = service.create("owner@x.com", 10L,
                new TaskRequest("Sans priorité", null, null, null, null));

        assertEquals(Priority.MEDIUM, response.priority());
        verify(notificationService, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void selfAssignmentDoesNotSendEmail() {
        service.create("owner@x.com", 10L, new TaskRequest("Pour moi", null, null, null, "owner@x.com"));

        verify(notificationService, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void assigneeMustBelongToWorkspace() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.create("owner@x.com", 10L,
                        new TaskRequest("T", null, null, null, "outsider@x.com")));

        assertEquals(400, ex.getStatusCode().value());
        verify(taskRepository, never()).save(any());
    }

    @Test
    void nonMemberCannotCreateTask() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.create("outsider@x.com", 10L, new TaskRequest("T", null, null, null, null)));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void unrelatedMemberCannotChangeStatus() {
        existingTask(member);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.updateStatus("other@x.com", 100L, TaskStatus.DONE));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void assigneeCanChangeStatus() {
        existingTask(member);

        TaskResponse response = service.updateStatus("member@x.com", 100L, TaskStatus.IN_PROGRESS);

        assertEquals(TaskStatus.IN_PROGRESS, response.status());
    }

    @Test
    void onlyCreatorOrAdminCanDelete() {
        existingTask(member);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.delete("member@x.com", 100L));

        assertEquals(403, ex.getStatusCode().value());
        verify(taskRepository, never()).deleteById(any());
    }

    @Test
    void creatorCanDelete() {
        existingTask(member);

        service.delete("owner@x.com", 100L);

        verify(commentRepository).deleteByTaskId(100L);
        verify(taskRepository).deleteById(100L);
    }

    @Test
    void changingDueDateResetsReminderFlag() {
        Task task = existingTask(member);
        task.setDueDate(LocalDate.now());
        task.setReminderSent(true);

        service.update("owner@x.com", 100L,
                new TaskRequest("T", null, Priority.LOW, LocalDate.now().plusDays(10), "member@x.com"));

        assertFalse(task.isReminderSent());
    }
}
