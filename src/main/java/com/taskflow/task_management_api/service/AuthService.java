package com.taskflow.task_management_api.service;

import com.taskflow.task_management_api.dto.*;
import com.taskflow.task_management_api.entity.User;
import com.taskflow.task_management_api.repository.UserRepository;
import com.taskflow.task_management_api.security.JwtService;
import com.taskflow.task_management_api.security.PasswordPolicy;
import com.taskflow.task_management_api.security.TotpService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    /** Même réponse que le compte existe déjà ou non : on ne révèle pas quelles adresses sont inscrites. */
    static final String REGISTER_MESSAGE =
            "Si l'adresse est valide, un e-mail de confirmation vient d'être envoyé. Clique sur le lien pour activer ton compte.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final TotpService totpService;
    private final AccountService accountService;
    private final NotificationService notificationService;

    public MessageResponse register(RegisterRequest request) {
        PasswordPolicy.validate(request.password(), request.email(), request.name());
        if (userRepository.findByEmail(request.email()).isPresent()) {
            notificationService.send(request.email(), "Tu as déjà un compte TaskFlow",
                    "Quelqu'un (peut-être toi) a tenté de créer un compte avec cette adresse, mais elle est déjà inscrite.\n"
                            + "Connecte-toi, ou utilise « Mot de passe oublié » si besoin. Sinon, ignore ce message.");
            return new MessageResponse(REGISTER_MESSAGE);
        }
        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .emailVerified(false)
                .build();
        userRepository.save(user);
        accountService.sendVerification(user);
        return new MessageResponse(REGISTER_MESSAGE);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        User user = userRepository.findByEmail(request.email()).orElse(null);
        if (user != null && !user.isEmailVerified()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Adresse e-mail non confirmée : clique sur le lien reçu par e-mail");
        }
        if (user != null && user.isTotpEnabled()) {
            return AuthResponse.challenge(jwtService.generateChallengeToken(request.email()));
        }
        return new AuthResponse(jwtService.generateToken(request.email()));
    }

    /** 2e étape : jeton de défi (obtenu après le mot de passe) + code à 6 chiffres. */
    public AuthResponse verifyTwoFactor(VerifyTwoFactorRequest request) {
        String email;
        try {
            email = jwtService.extractChallengeEmail(request.challengeToken());
        } catch (JwtException | IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session expirée, reconnecte-toi");
        }
        User user = userRepository.findByEmail(email).filter(User::isTotpEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Code invalide"));
        if (!totpService.verify(user.getTotpSecret(), request.code())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Code invalide");
        }
        return new AuthResponse(jwtService.generateToken(email));
    }
}
