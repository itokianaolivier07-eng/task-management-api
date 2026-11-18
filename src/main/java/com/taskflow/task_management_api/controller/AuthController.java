package com.taskflow.task_management_api.controller;

import com.taskflow.task_management_api.dto.*;
import com.taskflow.task_management_api.security.JwtAuthFilter;
import com.taskflow.task_management_api.service.AccountService;
import com.taskflow.task_management_api.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AccountService accountService;

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    @Value("${jwt.expiration}")
    private long expirationMillis;

    /** REGISTRATION_ENABLED=false ferme les inscriptions. */
    @Value("${app.registration.enabled:true}")
    private boolean registrationEnabled;

    /** ALLOWED_EMAIL_DOMAINS=societe.com,autre.org : seules ces adresses peuvent s'inscrire (vide = toutes). */
    @Value("${app.registration.allowed-email-domains:}")
    private List<String> allowedDomains;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse register(@Valid @RequestBody RegisterRequest request) {
        if (!registrationEnabled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Les inscriptions sont fermées");
        }
        String domain = request.email().substring(request.email().indexOf('@') + 1).toLowerCase(Locale.ROOT);
        boolean restricted = allowedDomains.stream().anyMatch(d -> !d.isBlank());
        if (restricted && allowedDomains.stream().noneMatch(d -> d.trim().equalsIgnoreCase(domain))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Inscriptions réservées à certains domaines e-mail");
        }
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        return startSession(authService.login(request), response);
    }

    @PostMapping("/verify-2fa")
    public AuthResponse verifyTwoFactor(@Valid @RequestBody VerifyTwoFactorRequest request,
                                        HttpServletResponse response) {
        return startSession(authService.verifyTwoFactor(request), response);
    }

    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyEmail(@Valid @RequestBody TokenRequest request) {
        accountService.verifyEmail(request.token());
    }

    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resendVerification(@Valid @RequestBody EmailRequest request) {
        accountService.resendVerification(request.email());
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forgotPassword(@Valid @RequestBody EmailRequest request) {
        accountService.forgotPassword(request.email());
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        accountService.resetPassword(request.token(), request.password());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString());
    }

    private AuthResponse startSession(AuthResponse auth, HttpServletResponse response) {
        if (auth.token() != null) {
            response.addHeader(HttpHeaders.SET_COOKIE,
                    cookie(auth.token(), Duration.ofMillis(expirationMillis)).toString());
        }
        return auth;
    }

    /** HttpOnly : illisible par JavaScript (donc par une faille XSS). Strict : jamais envoyé depuis un autre site. */
    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(JwtAuthFilter.COOKIE_NAME, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/api")
                .maxAge(maxAge)
                .build();
    }
}
