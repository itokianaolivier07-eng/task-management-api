package com.taskflow.task_management_api.controller;

import com.taskflow.task_management_api.entity.User;
import com.taskflow.task_management_api.repository.UserRepository;
import com.taskflow.task_management_api.security.TotpService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Activation / désactivation de la double authentification (routes protégées par JWT). */
@RestController
@RequestMapping("/api/2fa")
@RequiredArgsConstructor
public class TwoFactorController {

    private final UserRepository userRepository;
    private final TotpService totp;

    public record Status(boolean enabled) {}

    public record Setup(String secret, String otpauthUri) {}

    public record CodeRequest(@NotBlank @Pattern(regexp = "\\d{6}", message = "6 chiffres attendus") String code) {}

    @GetMapping("/status")
    public Status status(Authentication auth) {
        return new Status(user(auth).isTotpEnabled());
    }

    /** Étape 1 : génère un secret (non actif tant que /enable n'a pas reçu un code valide). */
    @PostMapping("/setup")
    public Setup setup(Authentication auth) {
        User user = user(auth);
        if (user.isTotpEnabled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La double authentification est déjà activée");
        }
        user.setTotpSecret(totp.newSecret());
        userRepository.save(user);
        return new Setup(user.getTotpSecret(), totp.otpauthUri("TaskFlow", user.getEmail(), user.getTotpSecret()));
    }

    /** Étape 2 : active après avoir vérifié un code produit par l'application d'authentification. */
    @PostMapping("/enable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enable(Authentication auth, @Valid @RequestBody CodeRequest request) {
        User user = user(auth);
        if (user.getTotpSecret() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lance d'abord la configuration");
        }
        if (!totp.verify(user.getTotpSecret(), request.code())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code invalide");
        }
        user.setTotpEnabled(true);
        userRepository.save(user);
    }

    @PostMapping("/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(Authentication auth, @Valid @RequestBody CodeRequest request) {
        User user = user(auth);
        if (!user.isTotpEnabled() || !totp.verify(user.getTotpSecret(), request.code())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code invalide");
        }
        user.setTotpEnabled(false);
        user.setTotpSecret(null);
        userRepository.save(user);
    }

    private User user(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
