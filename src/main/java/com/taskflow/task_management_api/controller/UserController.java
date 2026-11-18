package com.taskflow.task_management_api.controller;

import com.taskflow.task_management_api.dto.UserSummary;
import com.taskflow.task_management_api.repository.UserRepository;
import com.taskflow.task_management_api.service.AccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Profil et compte de l'utilisateur connecté. Routes protégées par défaut. */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final AccountService accountService;

    public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank @Size(max = 72) String newPassword) {}

    /** code : code TOTP à 6 chiffres, obligatoire si la double authentification est activée. */
    public record DeleteAccountRequest(@NotBlank String password, String code) {}

    @GetMapping("/me")
    public UserSummary me(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .map(UserSummary::of)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    @PostMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(Authentication auth, @Valid @RequestBody ChangePasswordRequest request) {
        accountService.changePassword(auth.getName(), request.currentPassword(), request.newPassword());
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(Authentication auth, @Valid @RequestBody DeleteAccountRequest request) {
        accountService.deleteAccount(auth.getName(), request.password(), request.code());
    }
}
