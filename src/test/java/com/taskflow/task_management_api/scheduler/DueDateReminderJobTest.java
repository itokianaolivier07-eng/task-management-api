package com.taskflow.task_management_api.scheduler;

import com.taskflow.task_management_api.entity.*;
import com.taskflow.task_management_api.repository.TaskRepository;
import com.taskflow.task_management_api.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DueDateReminderJobTest {

    @Mock TaskRepository taskRepository;
    @Mock NotificationService notificationService;
    @InjectMocks DueDateReminderJob job;

    @Test
    void sendsReminderAndMarksTask() {
        User assignee = User.builder().id(2L).name("Bob").email("bob@x.com").password("x").build();
        Project project = Project.builder().id(1L).name("P").build();
        Task task = Task.builder().id(5L).title("Rendre le rapport").assignee(assignee).project(project)
                .dueDate(LocalDate.now()).status(TaskStatus.TODO).priority(Priority.HIGH).build();
        when(taskRepository.findDueBetween(any(), any(), eq(TaskStatus.DONE))).thenReturn(List.of(task));

        job.sendReminders();

        verify(notificationService).send(eq("bob@x.com"), contains("Rappel"), anyString());
        assertTrue(task.isReminderSent());
    }
}
