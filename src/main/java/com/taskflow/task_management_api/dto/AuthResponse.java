package com.taskflow.task_management_api.dto;

/**
 * Connexion réussie : token (aussi posé en cookie HttpOnly par le contrôleur).
 * Double authentification requise : token = null, twoFactorRequired = true et challengeToken à renvoyer
 * avec le code à 6 chiffres sur /api/auth/verify-2fa.
 */
public record AuthResponse(String token, boolean twoFactorRequired, String challengeToken) {

    public AuthResponse(String token) {
        this(token, false, null);
    }

    public static AuthResponse challenge(String challengeToken) {
        return new AuthResponse(null, true, challengeToken);
    }
}
