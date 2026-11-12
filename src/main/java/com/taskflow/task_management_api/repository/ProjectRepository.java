package com.taskflow.task_management_api.repository;

import com.taskflow.task_management_api.entity.Project;
import com.taskflow.task_management_api.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByWorkspaceOrderByCreatedAtDesc(Workspace workspace);
}
