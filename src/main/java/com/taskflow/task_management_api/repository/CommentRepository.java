package com.taskflow.task_management_api.repository;

import com.taskflow.task_management_api.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("select c from Comment c join fetch c.author where c.task.id = :taskId order by c.createdAt asc")
    List<Comment> findAllByTask(@Param("taskId") Long taskId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Comment c where c.task.id = :taskId")
    void deleteByTaskId(@Param("taskId") Long taskId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Comment c where c.task.id in (select t.id from Task t where t.project.id = :projectId)")
    void deleteByProjectId(@Param("projectId") Long projectId);
}
