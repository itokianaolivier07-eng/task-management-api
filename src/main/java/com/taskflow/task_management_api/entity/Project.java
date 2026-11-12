package com.taskflow.task_management_api.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "projects")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Workspace workspace;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User createdBy;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
