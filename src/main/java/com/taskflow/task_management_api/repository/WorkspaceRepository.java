package com.taskflow.task_management_api.repository;

import com.taskflow.task_management_api.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {}