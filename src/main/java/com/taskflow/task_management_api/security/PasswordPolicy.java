package com.taskflow.task_management_api.security;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Set;

/** 12 à 72 caractères (72 = limite de BCrypt), au moins une lettre et un chiffre, ni courant ni répétitif. */
public final class PasswordPolicy {

    private static final Set<String> COMMON = Set.of(
            "password1234", "password12345", "password123456", "motdepasse123", "motdepasse1234",
            "azertyuiop123", "azerty123456", "qwertyuiop123", "qwerty123456", "123456789012",
            "1234567890ab", "administrator1", "welcome12345", "letmein12345", "iloveyou1234",
            "taskflow1234", "changeme1234", "abcdefghij12");

    private PasswordPolicy() {}

    public static void validate(String password, String email, String name) {
        if (password == null || password.length() < 12 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw bad("Le mot de passe doit contenir entre 12 et 72 caractères (72 octets maximum : les accents comptent double)");
        }
        boolean letter = false;
        boolean digit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) letter = true;
            if (Character.isDigit(c)) digit = true;
        }
        if (!letter || !digit) {
            throw bad("Le mot de passe doit contenir au moins une lettre et un chiffre");
        }
        String lower = password.toLowerCase(Locale.ROOT);
        if (COMMON.contains(lower) || lower.chars().distinct().count() < 5) {
            throw bad("Mot de passe trop courant ou trop répétitif");
        }
        String local = email == null ? "" : email.toLowerCase(Locale.ROOT).split("@")[0];
        if (local.length() >= 4 && lower.contains(local)) {
            throw bad("Le mot de passe ne doit pas contenir ton adresse e-mail");
        }
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
