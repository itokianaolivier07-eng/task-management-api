package com.taskflow.task_management_api.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    /** Secret TOTP (Base32). Non actif tant que totpEnabled = false. */
    @Column(name = "totp_secret", length = 64)
    private String totpSecret;

    @Builder.Default
    @Column(name = "totp_enabled", nullable = false, columnDefinition = "boolean not null default false")
    private boolean totpEnabled = false;

    /** Adresse confirmée par un lien reçu par e-mail. Les comptes existants sont considérés comme confirmés. */
    @Builder.Default
    @Column(name = "email_verified", nullable = false, columnDefinition = "boolean not null default true")
    private boolean emailVerified = false;

    /** Les sessions (JWT) émises avant cet instant sont refusées : changement de mot de passe, suppression… */
    @Column(name = "sessions_valid_after")
    private java.time.Instant sessionsValidAfter;
}