package com.taskflow.task_management_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** Jeton à usage unique (confirmation d'e-mail, réinitialisation de mot de passe). Seul son hash est stocké. */
@Entity
@Table(name = "user_tokens")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserToken {

    public enum Type { EMAIL_VERIFICATION, PASSWORD_RESET }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
