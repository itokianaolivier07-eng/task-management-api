package com.taskflow.task_management_api.controller;

import com.taskflow.task_management_api.dto.UserSummary;
import com.taskflow.task_management_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Profil de l'utilisateur connecté (le front affiche son nom). Route protégée par défaut. */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public UserSummary me(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .map(UserSummary::of)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
