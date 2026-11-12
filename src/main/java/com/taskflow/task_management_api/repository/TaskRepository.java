package com.taskflow.task_management_api.repository;

import com.taskflow.task_management_api.entity.Task;
import com.taskflow.task_management_api.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("select t from Task t left join fetch t.assignee join fetch t.createdBy "
         + "where t.project.id = :projectId order by t.dueDate asc, t.id desc")
    List<Task> findAllByProject(@Param("projectId") Long projectId);

    @Query("select t from Task t join fetch t.assignee join fetch t.createdBy join fetch t.project "
         + "where t.assignee.id = :userId order by t.dueDate asc, t.id desc")
    List<Task> findAssignedTo(@Param("userId") Long userId);

    @Query("select t from Task t join fetch t.assignee join fetch t.project "
         + "where t.dueDate between :from and :to and t.status <> :done and t.reminderSent = false")
    List<Task> findDueBetween(@Param("from") LocalDate from,
                              @Param("to") LocalDate to,
                              @Param("done") TaskStatus done);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Task t where t.project.id = :projectId")
    void deleteByProjectId(@Param("projectId") Long projectId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Task t set t.assignee = null where t.assignee.id = :userId "
         + "and t.project.id in (select p.id from Project p where p.workspace.id = :workspaceId)")
    void unassignUserInWorkspace(@Param("workspaceId") Long workspaceId, @Param("userId") Long userId);
}
