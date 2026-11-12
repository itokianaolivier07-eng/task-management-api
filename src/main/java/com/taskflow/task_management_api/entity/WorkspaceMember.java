package com.taskflow.task_management_api.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "workspace_members",
       uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "user_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkspaceMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Workspace workspace;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
}