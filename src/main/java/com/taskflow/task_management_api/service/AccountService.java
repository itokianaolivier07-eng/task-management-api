package com.taskflow.task_management_api.service;

import com.taskflow.task_management_api.entity.Role;
import com.taskflow.task_management_api.entity.User;
import com.taskflow.task_management_api.entity.UserToken;
import com.taskflow.task_management_api.entity.WorkspaceMember;
import com.taskflow.task_management_api.repository.TaskRepository;
import com.taskflow.task_management_api.repository.UserRepository;
import com.taskflow.task_management_api.repository.WorkspaceMemberRepository;
import com.taskflow.task_management_api.security.PasswordPolicy;
import com.taskflow.task_management_api.security.TotpService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Confirmation d'e-mail, mot de passe oublié / modifié, suppression de compte. */
@Service
public class AccountService {

    private static final Duration VERIFY_TTL = Duration.ofHours(24);
    private static final Duration RESET_TTL = Duration.ofMinutes(30);

    private final UserRepository userRepository;
    private final UserTokenService tokens;
    private final NotificationService notifications;
    private final PasswordEncoder passwordEncoder;
    private final TotpService totp;
    private final WorkspaceMemberRepository memberRepository;
    private final TaskRepository taskRepository;
    private final String frontendUrl;

    public AccountService(UserRepository userRepository, UserTokenService tokens, NotificationService notifications,
                          PasswordEncoder passwordEncoder, TotpService totp,
                          WorkspaceMemberRepository memberRepository, TaskRepository taskRepository,
                          @Value("${app.frontend-url}") String frontendUrl) {
        this.userRepository = userRepository;
        this.tokens = tokens;
        this.notifications = notifications;
        this.passwordEncoder = passwordEncoder;
        this.totp = totp;
        this.memberRepository = memberRepository;
        this.taskRepository = taskRepository;
        this.frontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
    }

    // ------------------------------------------------------------ confirmation d'e-mail

    @Transactional
    public void sendVerification(User user) {
        String token = tokens.create(user, UserToken.Type.EMAIL_VERIFICATION, VERIFY_TTL);
        notifications.send(user.getEmail(), "Confirme ton adresse e-mail TaskFlow",
                "Bonjour " + user.getName() + ",\n\nConfirme ton adresse e-mail avec ce lien (valable 24 h) :\n"
                        + link("/verify-email", token)
                        + "\n\nSi tu n'es pas à l'origine de cette inscription, ignore ce message.");
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = tokens.consume(token, UserToken.Type.EMAIL_VERIFICATION);
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    /** Ne révèle jamais si l'adresse existe : le contrôleur répond toujours 204. */
    @Transactional
    public void resendVerification(String email) {
        userRepository.findByEmail(email).filter(u -> !u.isEmailVerified()).ifPresent(this::sendVerification);
    }

    // ------------------------------------------------------------ mot de passe oublié / modifié

    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = tokens.create(user, UserToken.Type.PASSWORD_RESET, RESET_TTL);
            notifications.send(user.getEmail(), "Réinitialisation de ton mot de passe TaskFlow",
                    "Bonjour " + user.getName() + ",\n\nPour choisir un nouveau mot de passe (lien valable 30 min) :\n"
                            + link("/reset-password", token)
                            + "\n\nSi tu n'as rien demandé, ignore ce message : ton mot de passe ne change pas.");
        });
    }

    /** Si le nouveau mot de passe est refusé, la transaction est annulée et le jeton reste utilisable. */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = tokens.consume(token, UserToken.Type.PASSWORD_RESET);
        PasswordPolicy.validate(newPassword, user.getEmail(), user.getName());
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setEmailVerified(true); // il a prouvé qu'il contrôle la boîte mail
        user.setSessionsValidAfter(Instant.now()); // déconnecte les sessions existantes
        userRepository.save(user);
        notifications.send(user.getEmail(), "Ton mot de passe TaskFlow a été modifié",
                "Ton mot de passe vient d'être réinitialisé. Si ce n'est pas toi, réinitialise-le tout de suite.");
    }

    @Transactional
    public void changePassword(String email, String current, String next) {
        User user = find(email);
        if (!passwordEncoder.matches(current, user.getPassword())) {
            throw bad("Mot de passe actuel incorrect");
        }
        if (current.equals(next)) {
            throw bad("Le nouveau mot de passe doit être différent de l'actuel");
        }
        PasswordPolicy.validate(next, user.getEmail(), user.getName());
        user.setPassword(passwordEncoder.encode(next));
        user.setSessionsValidAfter(Instant.now());
        userRepository.save(user);
        notifications.send(user.getEmail(), "Ton mot de passe TaskFlow a été modifié",
                "Ton mot de passe vient d'être modifié. Si ce n'est pas toi, réinitialise-le tout de suite.");
    }

    // ------------------------------------------------------------ suppression de compte

    /**
     * Anonymise le compte (les tâches et commentaires restent, signés « Utilisateur supprimé »), retire l'utilisateur
     * de ses workspaces et déconnecte toutes ses sessions. Refusé s'il est encore OWNER d'un workspace.
     */
    @Transactional
    public void deleteAccount(String email, String password, String code) {
        User user = find(email);
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw bad("Mot de passe incorrect");
        }
        if (user.isTotpEnabled() && !totp.verify(user.getTotpSecret(), code)) {
            throw bad("Code de double authentification invalide");
        }
        List<WorkspaceMember> memberships = memberRepository.findByUser(user);
        if (memberships.stream().anyMatch(m -> m.getRole() == Role.OWNER)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Tu es propriétaire d'un workspace : supprime-le d'abord");
        }
        // Les ids sont lus avant toute requête de modification (celles-ci vident le contexte de persistance).
        Long userId = user.getId();
        List<Long> workspaceIds = memberships.stream().map(m -> m.getWorkspace().getId()).toList();
        memberRepository.deleteAll(memberships);
        memberRepository.flush();
        for (Long workspaceId : workspaceIds) {
            taskRepository.unassignUserInWorkspace(workspaceId, userId);
        }
        tokens.deleteAllFor(userId);
        user.setName("Utilisateur supprimé");
        user.setEmail("deleted-" + user.getId() + "@deleted.invalid");
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setTotpSecret(null);
        user.setTotpEnabled(false);
        user.setEmailVerified(false);
        user.setSessionsValidAfter(Instant.now());
        userRepository.save(user);
        notifications.send(email, "Ton compte TaskFlow a été supprimé", "Ton compte a bien été supprimé.");
    }

    // ------------------------------------------------------------ utilitaires

    private User find(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private String link(String path, String token) {
        return frontendUrl + path + "?token=" + token;
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
