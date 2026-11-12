package com.taskflow.task_management_api.repository;

import com.taskflow.task_management_api.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {
    List<WorkspaceMember> findByUser(User user);
    Optional<WorkspaceMember> findByWorkspaceAndUser(Workspace workspace, User user);
    List<WorkspaceMember> findByWorkspace(Workspace workspace);
    void deleteByWorkspace(Workspace workspace);
}
