package com.taskflow.task_management_api.service;

import com.taskflow.task_management_api.entity.User;
import com.taskflow.task_management_api.entity.UserToken;
import com.taskflow.task_management_api.repository.UserTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/** Jetons aléatoires à usage unique. Le jeton brut n'est envoyé que par e-mail ; la base ne garde que son SHA-256. */
@Service
@RequiredArgsConstructor
public class UserTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserTokenRepository repository;

    /** Crée un jeton (remplace l'éventuel jeton précédent du même type) et renvoie sa valeur brute. */
    @Transactional
    public String create(User user, UserToken.Type type, Duration ttl) {
        repository.deleteExpired(Instant.now());
        repository.deleteByUserAndType(user.getId(), type);
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        repository.save(UserToken.builder()
                .user(user).type(type).tokenHash(hash(raw)).expiresAt(Instant.now().plus(ttl)).build());
        return raw;
    }

    /** Vérifie le jeton, le supprime (usage unique) et renvoie son propriétaire. À appeler dans une transaction. */
    @Transactional
    public User consume(String raw, UserToken.Type type) {
        UserToken token = repository.findByTokenHash(hash(raw))
                .filter(t -> t.getType() == type && t.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lien invalide ou expiré"));
        User user = token.getUser();
        repository.delete(token);
        return user;
    }

    @Transactional
    public void deleteAllFor(Long userId) {
        repository.deleteAllForUser(userId);
    }

    static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
