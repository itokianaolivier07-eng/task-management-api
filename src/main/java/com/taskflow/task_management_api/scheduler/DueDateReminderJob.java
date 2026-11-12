package com.taskflow.task_management_api.scheduler;

import com.taskflow.task_management_api.entity.Task;
import com.taskflow.task_management_api.entity.TaskStatus;
import com.taskflow.task_management_api.repository.TaskRepository;
import com.taskflow.task_management_api.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** Envoie un rappel pour les tâches non terminées dont l'échéance est aujourd'hui ou demain. */
@Component
@RequiredArgsConstructor
@Slf4j
public class DueDateReminderJob {

    private final TaskRepository taskRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "${app.reminders.cron}")
    @Transactional
    public void sendReminders() {
        LocalDate today = LocalDate.now();
        List<Task> tasks = taskRepository.findDueBetween(today, today.plusDays(1), TaskStatus.DONE);
        for (Task task : tasks) {
            notificationService.send(
                    task.getAssignee().getEmail(),
                    "Rappel : échéance proche pour « " + task.getTitle() + " »",
                    "Bonjour " + task.getAssignee().getName() + ",\n\n"
                            + "La tâche « " + task.getTitle() + " » (projet " + task.getProject().getName()
                            + ") est à rendre le " + task.getDueDate() + ".\n"
                            + "Statut actuel : " + task.getStatus() + "\n");
            task.setReminderSent(true);
        }
        log.info("{} rappel(s) d'échéance envoyé(s)", tasks.size());
    }
}
